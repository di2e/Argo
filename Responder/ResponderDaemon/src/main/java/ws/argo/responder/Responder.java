/*
 * Copyright 2015 Jeff Simpson.
 *
 * Licensed under the MIT License, (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.argo.responder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.Instant;

import ws.argo.responder.transport.ProbeProcessor;
import ws.argo.responder.transport.Transport;
import ws.argo.responder.transport.TransportConfigException;
import ws.argo.wireline.probe.ProbeWrapper;

/**
 * This is the main class for the Responder system. It will start up the
 * multicast socket and begin listening for incoming probes. If it gets a well
 * formed probe, it will launch a {@link ProbeHandlerThread}. This allows the
 * Responder to process as many probes as possible.
 * 
 * @author jmsimpson
 *
 */
public class Responder implements ProbeProcessor {

  private static final String VERSION_PROPERTIES = "/version.properties";

  private static final Logger LOGGER = Logger.getLogger(Responder.class.getName());

  private static String ARGO_VERSION = "UNKNOWN";

  private ArrayList<Transport> transports = new ArrayList<Transport>();

  private ArrayList<ProbeHandlerPluginIntf> handlers = new ArrayList<ProbeHandlerPluginIntf>();

  // 30 second timeout - the processing loop will interrupt once every 30
  // seconds to check to see if the loop should quit. This is for hygiene as
  // well as unit/integration tests
  // private static int INBOUND_SOCKET_TIMEOUT = 30000;

  // flag to tell the processing loop to continue or not.
  // this allows external control on the processing loop so you don't have to
  // directly
  // kill the process to stop a Responder - see inboundSocketTimeout
  private boolean shouldRun = true;

  private NetworkInterface  ni            = null;
  protected MulticastSocket inboundSocket = null;
  protected InetAddress     maddress;

  protected CloseableHttpClient httpClient;

  private ResponderConfigurationBean cliValues;

  private ResponderShutdown shutdownHook;

  // This id is for internal reporting and logging reasons
  private String runtimeId;

  ThreadPoolExecutor     executorPool;
  ResponderMonitorThread monitor;

  ConcurrentLinkedQueue<Instant> messages = new ConcurrentLinkedQueue<Instant>();

  /**
   * Utility class to encaptulate some of the Responder configuration.
   * 
   * @author jmsimpson
   *
   */
  private static class ResponderConfigurationBean {

    public int                     multicastPort     = 4003;
    public String                  multicastAddress  = "230.0.0.1";
    public boolean                 noBrowser         = false;
    public ArrayList<PluginConfig> appHandlerConfigs = new ArrayList<PluginConfig>();
    public ArrayList<PluginConfig> transportConfigs  = new ArrayList<PluginConfig>();
    public String                  networkInterface;
    public boolean                 runMonitor;
    public int                     monitorInterval;
    public int                     threadPoolSize;

  }

  /**
   * Utility class to encapsulate the configuration of the plugin information.
   * The plugins include the app handler and the transports.
   * 
   * @author jmsimpson
   *
   */
  private static class PluginConfig {
    public String classname;
    public String configFilename;
  }

  /**
   * Shutdown hook handler for the Responder. See
   * {@linkplain Runtime#addShutdownHook(Thread)}.
   * 
   * @author jmsimpson
   *
   */
  public static class ResponderShutdown extends Thread {
    Responder agent;

    public ResponderShutdown(Responder agent) {
      this.agent = agent;
      agent.setShutdownHook(this);
    }

    /**
     * Runs the shutdown hook. Closes any open ports before quitting the main
     * java process thread. This ostensibly avoids leaving dangling resources.
     */
    public void run() {
      agent.shutdown();
    }
  }

  /**
   * Create a new instance of a Responder.
   * 
   * @param cliValues - the list of command line arguments
   */
  public Responder(ResponderConfigurationBean cliValues) {
    this.cliValues = cliValues;
    httpClient = HttpClients.createDefault();
    UUID uuid = UUID.randomUUID();
    runtimeId = uuid.toString();

    intializeThreadPool();
  }

  private void intializeThreadPool() {
    // RejectedExecutionHandler implementation
    RejectedExecutionHandler rejectionHandler = new RejectedExecutionHandlerImpl();
    // Get the ThreadFactory implementation to use
    ThreadFactory threadFactory = Executors.defaultThreadFactory();
    // creating the ThreadPoolExecutor

    executorPool = new ThreadPoolExecutor(cliValues.threadPoolSize, cliValues.threadPoolSize + 2, 4, TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(cliValues.threadPoolSize * 2), threadFactory, rejectionHandler);

    // start the monitoring thread
    if (cliValues.runMonitor)
      monitor = new ResponderMonitorThread(this, executorPool, cliValues.monitorInterval);
    Thread monitorThread = new Thread(monitor);
    monitorThread.start();

  }

  public ArrayList<ProbeHandlerPluginIntf> getHandlers() {
    return handlers;
  }

  /**
   * Add a new handler given the classname and config filename. It instantiates
   * the class and then calls its initialization method to get the handler ready
   * to process inbound probes.
   * 
   * @param classname is the FQCN of the handler class
   * @param configFilename is the full path name of the config file name
   *          specific for the handler (could be any crazy format)
   * @throws ResponderConfigException if there is some issues with the config
   *           file
   */
  private void addHandler(String classname, String configFilename) throws ResponderConfigException {

    ClassLoader cl = ClassLoader.getSystemClassLoader();
    Class<?> handlerClass;
    try {
      handlerClass = cl.loadClass(classname);
    } catch (ClassNotFoundException e1) {
      throw new ResponderConfigException("Error loading the handler class", e1);
    }
    ProbeHandlerPluginIntf handler;

    try {
      handler = (ProbeHandlerPluginIntf) handlerClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      LOGGER.warning("Could not create an instance of the configured handler class - " + classname);
      throw new ResponderConfigException("Error instantiating the handler class " + classname, e);
      // LOGGER.warning("Using default handler");
      // LOGGER.fine("The issue was:");
      // LOGGER.fine(e.getMessage());
      // handler = new ConfigFileProbeHandlerPluginImpl();
    }

    handler.initializeWithPropertiesFilename(configFilename);

    handlers.add(handler);
  }
  
  /**
   * Add a new handler given the classname and config filename. It instantiates
   * the class and then calls its initialization method to get the handler ready
   * to process inbound probes.
   * 
   * @param classname is the FQCN of the handler class
   * @param configFilename is the full path name of the config file name
   *          specific for the handler (could be any crazy format)
   * @throws ResponderConfigException if there is some issues with the config
   *           file
   * @throws TransportConfigException if the transport configuration failed
   */
  private void addTransport(String classname, String configFilename) throws ResponderConfigException, TransportConfigException {

    ClassLoader cl = ClassLoader.getSystemClassLoader();
    Class<?> transportClass;
    try {
      transportClass = cl.loadClass(classname);
    } catch (ClassNotFoundException e1) {
      throw new ResponderConfigException("Error loading the transport class", e1);
    }
    Transport transport;

    try {
      transport = (Transport) transportClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      LOGGER.warning("Could not create an instance of the configured transport class - " + classname);
      throw new ResponderConfigException("Error instantiating the transport class " + classname, e);
    }

    transport.initialize(this, configFilename);

    transports.add(transport);
  }

  /**
   * Close the socket and tell the processing loop to terminate. This is a
   * forced shutdown rather then a natural shutdown. A natural shutdown happens
   * when the shutdown hook fires when the VM exists. This method forces all
   * that to happen. This is done to allow multiple Responders to be created and
   * destroyed during a single VM session. Necessary for various testing and
   * management procedures.
   */
  public void stopResponder() {
    LOGGER.info("Force shutdown of Responder [" + runtimeId + "]");
    shouldRun = false;
    shutdownHook.start();
    Runtime.getRuntime().removeShutdownHook(shutdownHook);
  }

  /**
   * This will shutdown the listening socket and remove the responder from the
   * multicast group. Part of the natural lifecycle. It also will end the run
   * loop of the responder automatically - it will interrupt any read operation
   * going on and exit the run loop.
   */
  public void shutdown() {
    LOGGER.info("Responder shutting down port " + cliValues.multicastPort + " [" + runtimeId + "]");
    if (inboundSocket != null) {
      try {
        inboundSocket.leaveGroup(maddress);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Error leaving multicast group", e);
      }
      inboundSocket.close();

    }
  }

  public void setShutdownHook(ResponderShutdown shutdownHook) {
    this.shutdownHook = shutdownHook;
  }

  /**
   * This is the main run method for the Argo Responder. It starts up all the
   * necessary machinery and enters the UDP receive loop.
   * 
   * @throws ResponderOperationException if some IOException or other
   *           operational problem occurs
   * 
   */
  public void run() throws ResponderOperationException {

    Thread transportThread;
    for (Transport t : transports) {
      
      transportThread = new Thread(t);
      transportThread.setName(t.transportName());
      transportThread.start();
            
    }
    
    
/*    DatagramPacket packet;

    LOGGER.fine("Starting Responder loop - infinite until process terminated");
    // infinite loop until the responder is terminated
    while (shouldRun) {

      byte[] buf = new byte[2 * 1024]; // TODO parameterize the buffer size
      packet = new DatagramPacket(buf, buf.length);
      LOGGER.fine("Waiting to recieve packet...");
      try {
        inboundSocket.receive(packet);

        LOGGER.fine("Received packet");
        LOGGER.fine("Packet contents:");

        // Get the actual wireline payload
        String probeStr = new String(packet.getData(), 0, packet.getLength());
        LOGGER.fine(probeStr);

        try {
          XMLSerializer serializer = new XMLSerializer();

          ProbeWrapper probe = serializer.unmarshal(probeStr);

          // reuses the handlers and the httpClient. Both should be threadSafe
          executorPool.execute(new ProbeHandlerThread(this, probe, cliValues.noBrowser));

          // Thread t = new Thread(new ProbeHandlerThread(this, probe,
          // cliValues.noBrowser));
          // t.start();
        } catch (ProbeParseException e) {
          LOGGER.log(Level.SEVERE, "Error parsing inbound probe payload.", e);
        }
      } catch (SocketTimeoutException toe) {
        LOGGER.finest("Responder loop timeout fired.");
      } catch (IOException e1) {
        if (shouldRun) {
          throw new ResponderOperationException("Error during responder wireline read loop.", e1);
        }
      }

    }

    LOGGER.info("Stopping responder through trigger [" + runtimeId + "]");*/

  }

  @Override
  /**
   * This is where the rubber meets the road. The transport module has
   */
  public void processProbe(ProbeWrapper probe) {
    executorPool.execute(new ProbeHandlerThread(this, probe, cliValues.noBrowser));
  }

  /**
   * Calculates the number of probes per second over the last 1000 probes.
   * 
   * @return probes per second
   */
  public synchronized float probesPerSecond() {
    Instant[] m = messages.toArray(new Instant[messages.size()]);

    if (m.length == 0)
      return 0;

    int offset = 1;
    Instant oldest = m[0];
    Instant newest = m[m.length - offset];

    while (newest == null) {
      offset++;
      newest = m[m.length - offset];
    }

    float seconds = (float) ((newest.getMillis() - oldest.getMillis()) / 1000.0);
    // System.out.println(oldest + ":" + newest + " - " + m.length + " - " +
    // seconds);
    float mps = m.length / seconds;

    return mps;

  }

  public int probesProcessed() {
    return messages.size();
  }

  /**
   * Tells the Responder that a message was responded to.
   */
  public void probeProcessed() {
    messages.add(new Instant());

    long size = messages.size();
    if (size > 1000)
      messages.poll();
  }

  private void loadHandlerPlugins(ArrayList<PluginConfig> configs) throws ResponderConfigException {

    for (PluginConfig appConfig : configs) {

      try {
        addHandler(appConfig.classname, appConfig.configFilename);
      } catch (ResponderConfigException e) {
        LOGGER.log(Level.SEVERE, "Error loading handler for [" + appConfig.classname + "]. Skipping handler", e);
      }
    }

    // make sure we have at least 1 active handler. If not, then fail the
    // responder process
    if (getHandlers().isEmpty()) {
      throw new ResponderConfigException("No responders created successfully on initialization.");
    }

  }
  
  private void loadTransportPlugins(ArrayList<PluginConfig> configs) throws ResponderConfigException {

    for (PluginConfig appConfig : configs) {

      try {
        addTransport(appConfig.classname, appConfig.configFilename);
      } catch (ResponderConfigException | TransportConfigException e) {
        LOGGER.log(Level.SEVERE, "Error loading handler for [" + appConfig.classname + "]. Skipping handler", e);
      }
    }

    // make sure we have at least 1 active handler. If not, then fail the
    // responder process
    if (transports.isEmpty()) {
      throw new ResponderConfigException("No responders created successfully on initialization.");
    }

  }

  /**
   * This method attempts to join the multicast group in a particular Network
   * Interface (NI). This is useful for when inbound multicast ONLY can occur on
   * a particular interface channel. However, if there is some issue with the NI
   * name, then it attempts to join on the localhost NI. It is possible,
   * however, unlikely that this will fail as well. This happens when strange
   * things are happening to the routing tables and the presentation of the NIs
   * in the OS. This can happen with you have VPN clients or hypervisors running
   * on the host OS ... so look out.
   * 
   * @return true if the join was successful
   */
  private boolean joinGroup() {
    boolean success = true;
    InetSocketAddress socketAddress = new InetSocketAddress(cliValues.multicastAddress, cliValues.multicastPort);
    try {
      // Setup for incoming multicast requests
      maddress = InetAddress.getByName(cliValues.multicastAddress);

      if (cliValues.networkInterface != null) {
        ni = NetworkInterface.getByName(cliValues.networkInterface);
      }
      if (ni == null) {
        InetAddress localhost = InetAddress.getLocalHost();
        LOGGER.fine("Network Interface name not specified.  Using the NI for localhost [" + localhost.getHostAddress() + "]");
        ni = NetworkInterface.getByInetAddress(localhost);
        if (ni != null && ni.isLoopback()) {
          LOGGER.warning("DEFAULT NETWORK INTERFACE IS THE LOOPBACK !!!!.");
          LOGGER.warning("Attempting to use the NI for localhost [" + ni.getName() + "] is a loopback.");
          LOGGER.warning("Please run the Responder with the -ni switch selecting a more appropriate network interface to use (e.g. -ni eth0).");
          return false;
        }
      }

      LOGGER.info("Starting Responder:  Receiving mulitcast @ [" + cliValues.multicastAddress + ":" + cliValues.multicastPort + "]");
      this.inboundSocket = new MulticastSocket(cliValues.multicastPort);

      if (ni == null) { // for some reason NI is still NULL. Not sure why
        // this happens.
        this.inboundSocket.joinGroup(maddress);
        LOGGER.warning("Unable to determine the network interface for the localhost address.  Check /etc/hosts for weird entry like 127.0.1.1 mapped to DNS name.");
        LOGGER.info("Unknown network interface joined group [" + socketAddress.toString() + "]");
      } else {
        this.inboundSocket.joinGroup(socketAddress, ni);
        LOGGER.info(ni.getName() + " joined group " + socketAddress.toString());
      }
    } catch (IOException e) {
      if (ni == null) {
        LOGGER.log(Level.SEVERE, "Error attempting to joint multicast address.", e);
      } else {
        StringBuffer buf = new StringBuffer();
        try {
          buf.append("(lb:" + this.ni.isLoopback() + " ");
        } catch (SocketException e2) {
          buf.append("(lb:err ");
        }
        try {
          buf.append("m:" + this.ni.supportsMulticast() + " ");
        } catch (SocketException e3) {
          buf.append("(m:err ");
        }
        try {
          buf.append("p2p:" + this.ni.isPointToPoint() + " ");
        } catch (SocketException e1) {
          buf.append("p2p:err ");
        }
        try {
          buf.append("up:" + this.ni.isUp() + " ");
        } catch (SocketException e1) {
          buf.append("up:err ");
        }
        buf.append("v:" + this.ni.isVirtual() + ") ");

        LOGGER.log(Level.SEVERE, ni.getName() + " " + buf.toString() + ": could not join group " + socketAddress.toString() + " --> " + e.toString(), e);
      }
      success = false;
    }
    return success;
  }

  /**
   * Main entry point for Argo Responder.
   * 
   * @param args command line arguments
   * @throws ResponderConfigException if bad things happen with the
   *           configuration files
   * @throws ResponderOperationException if a runtime error occurs
   */
  public static void main(String[] args) throws ResponderConfigException, ResponderOperationException {

    Responder responder = initialize(args);
    if (responder != null) {
      responder.run();
    }

  }

  /**
   * Create a new Responder given the command line arguments.
   * 
   * @param args - command line arguments
   * @return the new Responder instance or null if something wonky happened
   * @throws ResponderConfigException if bad things happen with the
   *           configuration files and the content of the files. For example if
   *           the classnames for the probe handlers are bad (usually a type or
   *           classpath issue)
   */
  public static Responder initialize(String[] args) throws ResponderConfigException {
    readVersionProperties();

    LOGGER.info("Starting Argo Responder daemon process. Version " + ARGO_VERSION);

    ResponderConfigurationBean cliValues = parseCommandLine(args);

    if (cliValues == null) {
      LOGGER.log(Level.SEVERE, "Invalid Responder Configuration.  Terminating Responder process.");
      return null;
    }

    Responder responder = new Responder(cliValues);

    // load up the handler classes specified in the configuration parameters
    responder.loadHandlerPlugins(cliValues.appHandlerConfigs);

    // load up the transport classes specified in the configuration parameters
    responder.loadTransportPlugins(cliValues.transportConfigs);

    LOGGER.info("Responder registering shutdown hook.");
    ResponderShutdown hook = new ResponderShutdown(responder);
    Runtime.getRuntime().addShutdownHook(hook);

    if (!responder.joinGroup()) {
      LOGGER.severe("Unable to join multicast group. Terminating Responder process.");
      return null;
    }

    // This needs to be sent to stdout as there is no way to force the logging
    // of this via the LOGGER
    System.out.println("Argo " + ARGO_VERSION + " :: " + "Responder started on " + cliValues.multicastAddress + ":" + cliValues.multicastPort + " [" + responder.runtimeId + "]");

    return responder;
  }

  private static void readVersionProperties() {
    InputStream is = Responder.class.getResourceAsStream(VERSION_PROPERTIES);
    if (is != null) {
      Properties p = new Properties();
      try {
        p.load(is);
        ARGO_VERSION = p.getProperty("argo-version");
      } catch (IOException e) {
        LOGGER.warning("Cannot load the file [" + VERSION_PROPERTIES + "]");
      } finally {
        try {
          is.close();
        } catch (IOException e) {
          LOGGER.warning("Cannot clost the file [" + VERSION_PROPERTIES + "] " + e.getMessage());
        }
      }

    } else {
      LOGGER.warning("Cannot load the file [" + VERSION_PROPERTIES + "]");
    }
  }

  private static ResponderConfigurationBean parseCommandLine(String[] args) throws ResponderConfigException {
    CommandLineParser parser = new BasicParser();
    ResponderConfigurationBean cliValues = null;

    // Process the help option
    try {
      CommandLine cl = parser.parse(getOptions(), args);

      if (cl.hasOption("h")) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Responder", getOptions());
        return null;
      }

      // This System.out call is actually required
      if (cl.hasOption("v")) {
        System.out.println("Argo Responder version: " + ARGO_VERSION);
        return null;
      }

      cliValues = processCommandLine(cl);

    } catch (UnrecognizedOptionException e) {
      LOGGER.log(Level.SEVERE, "Error parsing command line:  " + e.getLocalizedMessage());
    } catch (ParseException e) {
      LOGGER.log(Level.SEVERE, "Error parsing option.", e);
    }

    return cliValues;
  }

  private static ResponderConfigurationBean processCommandLine(CommandLine cl) throws ResponderConfigException {

    LOGGER.fine("Parsing command line values:");

    ResponderConfigurationBean propsConfig = new ResponderConfigurationBean();

    if (cl.hasOption("pf")) {
      String propsFilename = cl.getOptionValue("pf");
      try {
        propsConfig = processPropertiesValue(propsFilename, propsConfig);
      } catch (ResponderConfigException e) {
        LOGGER.log(Level.SEVERE, "Unable to read properties file named [" + propsFilename + "] due to:", e);
        throw e;
      }
    } else {
      LOGGER.warning("WARNING: no properties file specified.  Working off cli override arguments.");
    }

    // No browser option - if set then do not process naked probes
    if (cl.hasOption("nb")) {
      propsConfig.noBrowser = true;
      LOGGER.info("Responder started in no browser mode.");
    }

    // Network Interface
    if (cl.hasOption("ni")) {
      String ni = cl.getOptionValue("ni");
      propsConfig.networkInterface = ni;
    }

    if (cl.hasOption("mp")) {
      try {
        int portNum = Integer.parseInt(cl.getOptionValue("mp"));
        propsConfig.multicastPort = portNum;
        LOGGER.info("Overriding multicast port with command line value");
      } catch (NumberFormatException e) {
        throw new ResponderConfigException("The multicast port number [" + cl.getOptionValue("mp") + "]- is not formattable as an integer", e);
      }
    }

    if (cl.hasOption("ma")) {
      propsConfig.multicastAddress = cl.getOptionValue("ma");
      LOGGER.info("Overriding multicast address with command line value");
    }

    return propsConfig;

  }

  private static ResponderConfigurationBean processPropertiesValue(String propertiesFilename, ResponderConfigurationBean config) throws ResponderConfigException {
    Properties prop = new Properties();

    InputStream is = null;
    try {
      if (Responder.class.getResource(propertiesFilename) != null) {
        is = Responder.class.getResourceAsStream(propertiesFilename);
        LOGGER.info("Reading Responder properties file [" + propertiesFilename + "] from classpath.");
      } else {
        is = new FileInputStream(propertiesFilename);
        LOGGER.info("Reading Responder properties file [" + propertiesFilename + "] from file system.");
      }
      prop.load(is);
    } catch (FileNotFoundException e) {
      throw new ResponderConfigException(e.getLocalizedMessage(), e);
    } catch (IOException e) {
      throw new ResponderConfigException(e.getLocalizedMessage(), e);
    } finally {
      try {
        is.close();
      } catch (Exception e) {
        throw new ResponderConfigException(e.getLocalizedMessage(), e);
      }
    }

    try {
      int port = Integer.parseInt(prop.getProperty("multicastPort", "4003"));
      config.multicastPort = port;
    } catch (NumberFormatException e) {
      LOGGER.warning("Error reading port number from properties file.  Using default port of 4003.");
      config.multicastPort = 4003;
    }

    config.multicastAddress = prop.getProperty("multicastAddress", "230.0.0.1");

    config.runMonitor = Boolean.parseBoolean(prop.getProperty("runMonitor", "false"));

    try {
      int monitorInterval = Integer.parseInt(prop.getProperty("monitorInterval", "5"));
      config.monitorInterval = monitorInterval;
    } catch (NumberFormatException e) {
      LOGGER.warning("Error reading monitorInterval number from properties file.  Using default port of 5.");
      config.monitorInterval = 5;
    }

    try {
      int threadSize = Integer.parseInt(prop.getProperty("threadPoolSize", "10"));
      config.threadPoolSize = threadSize;
    } catch (NumberFormatException e) {
      LOGGER.warning("Error reading threadPoolSize number from properties file.  Using default threadPoolSize of 10.");
      config.threadPoolSize = 10;
    }

    // handle the list of appHandler information

    boolean continueProcessing = true;
    int number = 1;
    while (continueProcessing) {
      String appHandlerClassname;
      String configFilename;

      appHandlerClassname = prop.getProperty("probeHandlerClassname." + number, "ws.argo.responder.plugin.ConfigFileProbeHandlerPluginImpl");
      configFilename = prop.getProperty("probeHandlerConfigFilename." + number, null);

      if (configFilename != null) {
        PluginConfig handlerConfig = new PluginConfig();
        handlerConfig.classname = appHandlerClassname;
        handlerConfig.configFilename = configFilename;
        config.appHandlerConfigs.add(handlerConfig);
      } else {
        continueProcessing = false;
      }
      number++;

    }

    // handle the list of transport information

    continueProcessing = true;
    number = 1;
    while (continueProcessing) {
      String transportClassname;
      String configFilename;

      transportClassname = prop.getProperty("transportClassname." + number, "ws.argo.responder.transport.MulticastTransport");
      configFilename = prop.getProperty("transportConfigFilename." + number, null);

      if (configFilename != null) {
        PluginConfig handlerConfig = new PluginConfig();
        handlerConfig.classname = transportClassname;
        handlerConfig.configFilename = configFilename;
        config.transportConfigs.add(handlerConfig);
      } else {
        continueProcessing = false;
      }
      number++;

    }

    return config;

  }

  @SuppressWarnings("static-access")
  private static Options getOptions() {
    Options options = new Options();

    options.addOption("h", false, "display help for the Responder daemon");
    options.addOption("v", false, "display version for the Responder daemon");
    options.addOption(OptionBuilder.withArgName("networkInterface name")
        .hasArg()
        .withDescription("network interface name to listen on")
        .create("ni"));
    options.addOption(OptionBuilder.withArgName("properties filename")
        .hasArg().withType("")
        .withDescription("fully qualified properties filename")
        .create("pf"));
    options.addOption(OptionBuilder.withArgName("multicastPort").hasArg()
        .withType(Integer.valueOf(0))
        .withDescription("the multicast port to broadcast on")
        .create("mp"));
    options.addOption(OptionBuilder.withArgName("multicastAddr").hasArg()
        .withDescription("the multicast group address to broadcast on")
        .create("ma"));
    options.addOption(OptionBuilder
        .withDescription("setting this switch will disable the responder from returning all services to a naked probe")
        .create("nb"));

    return options;
  }

}
