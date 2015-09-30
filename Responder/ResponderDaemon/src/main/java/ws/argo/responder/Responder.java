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

  private static final String               VERSION_PROPERTIES = "/version.properties";

  private static final Logger               LOGGER             = Logger.getLogger(Responder.class.getName());

  private static String                     ARGO_VERSION       = "UNKNOWN";

  private ArrayList<Transport>              _transports        = new ArrayList<Transport>();

  private ArrayList<ProbeHandlerPluginIntf> _handlers          = new ArrayList<ProbeHandlerPluginIntf>();

  protected InetAddress                     maddress;

  protected CloseableHttpClient             httpClient;

  private ResponderConfigurationBean        _cliValues;

  private ResponderShutdown                 _shutdownHook;

  // This id is for internal reporting and logging reasons
  private String                            _runtimeId;

  private ThreadPoolExecutor                _executorPool;
  private ResponderMonitorThread            _monitor           = null;

  ConcurrentLinkedQueue<Instant>            messages           = new ConcurrentLinkedQueue<Instant>();

  /**
   * Utility class to encaptulate some of the Responder configuration.
   * 
   * @author jmsimpson
   *
   */
  private static class ResponderConfigurationBean {

    public boolean                 noBrowser         = false;
    public ArrayList<PluginConfig> appHandlerConfigs = new ArrayList<PluginConfig>();
    public ArrayList<PluginConfig> transportConfigs  = new ArrayList<PluginConfig>();
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
    this._cliValues = cliValues;
    httpClient = HttpClients.createDefault();
    UUID uuid = UUID.randomUUID();
    _runtimeId = uuid.toString();

    intializeThreadPool();
  }

  private void intializeThreadPool() {
    // RejectedExecutionHandler implementation
    RejectedExecutionHandler rejectionHandler = new RejectedExecutionHandlerImpl();
    // Get the ThreadFactory implementation to use
    ThreadFactory threadFactory = Executors.defaultThreadFactory();
    // creating the ThreadPoolExecutor

    _executorPool = new ThreadPoolExecutor(_cliValues.threadPoolSize, _cliValues.threadPoolSize + 2, 4, TimeUnit.SECONDS, 
        new ArrayBlockingQueue<Runnable>(_cliValues.threadPoolSize * 2), threadFactory, rejectionHandler);

    // start the monitoring thread
    if (_cliValues.runMonitor) {
      _monitor = new ResponderMonitorThread(this, _executorPool, _cliValues.monitorInterval);
      Thread monitorThread = new Thread(_monitor);
      monitorThread.start();
    }

  }

  @Override
  public String getRuntimeID() {
    return _runtimeId;
  }

  public ArrayList<ProbeHandlerPluginIntf> getHandlers() {
    return _handlers;
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

    _handlers.add(handler);
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

    _transports.add(transport);
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
    LOGGER.info("Force shutdown of Responder [" + _runtimeId + "]");
    _shutdownHook.start();
    Runtime.getRuntime().removeShutdownHook(_shutdownHook);
  }

  /**
   * This will shutdown the listening socket and remove the responder from the
   * multicast group. Part of the natural life cycle. It also will end the run
   * loop of the responder automatically - it will interrupt any read operation
   * going on and exit the run loop.
   */
  public void shutdown() {
    LOGGER.info("Responder shutting down: [" + _runtimeId + "]");
    for (Transport t : _transports) {
      t.shutdown();
    }
  }

  public void setShutdownHook(ResponderShutdown shutdownHook) {
    this._shutdownHook = shutdownHook;
  }

  /**
   * This is the main run method for the Argo Responder. It starts up all the
   * configured transports in their own thread and starts their receive loops.
   * 
   * <p>Transports run in their own thread. Thus, when all the transports are
   * running, this method will exit. You can shutdown the Responder by calling
   * the {@linkplain #shutdown()} method. This method will be called by the
   * {@linkplain ResponderShutdown} hook.
   * 
   * 
   */
  public void run() {

    // I hope that this hits you over the head with its simplicity.
    // That's the idea. The instances of the transports are supposed to be send
    // contained.

    Thread transportThread;
    for (Transport t : _transports) {

      transportThread = new Thread(t);
      transportThread.setName(t.transportName());
      transportThread.start();

    }

  }

  @Override
  /**
   * This is where the rubber meets the road. The transport module has
   */
  public void processProbe(ProbeWrapper probe) {
    _executorPool.execute(new ProbeHandlerThread(this, probe, _cliValues.noBrowser));
  }

  /**
   * Calculates the number of probes per second over the last 1000 probes.
   * 
   * @return probes per second
   */
  public synchronized float probesPerSecond() {

    Instant now = new Instant();

    Instant event = null;
    int events = 0;
    boolean done = false;
    long timeWindow = 0;
    long oldestTime = 0;

    do {
      event = messages.poll();
      if (event != null) {
        events++;
        if (events == 1)
          oldestTime = event.getMillis();
        done = event.getMillis() >= now.getMillis();
      } else {
        done = true;
      }
    } while (!done);

    timeWindow = now.getMillis() - oldestTime;

    float mps = (float) events / timeWindow;
    mps = (float) (mps * 1000.0);

    return mps;

  }

  public int probesProcessed() {
    return messages.size();
  }

  /**
   * Tells the Responder that a message was responded to.
   */
  public void probeProcessed() {
    if (_monitor != null) {
      messages.add(new Instant());
    }
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
    if (_transports.isEmpty()) {
      throw new ResponderConfigException("No responder transports created successfully on initialization.  There needs to be a least one transport instance for the Responder to work.");
    }

  }

  /**
   * Main entry point for Argo Responder.
   * 
   * @param args command line arguments
   * @throws ResponderConfigException if bad things happen with the
   *           configuration files
   * @throws ResponderOperationException if a runtime error occurs
   */
  public static void main(String[] args) throws ResponderConfigException {

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

    // This needs to be sent to stdout as there is no way to force the logging
    // of this via the LOGGER
    System.out.println("Argo " + ARGO_VERSION + " :: " + "Responder started  [" + responder._runtimeId + "]");

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
