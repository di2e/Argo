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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

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

import ws.argo.responder.plugin.configfile.ConfigFileProbeHandlerPluginImpl;
import ws.argo.wireline.probe.ProbeParseException;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.probe.XMLSerializer;

public class Responder {

  private static final Logger                      LOGGER        = Logger.getLogger(Responder.class.getName());

  private static String                            ARGO_VERSION  = "0.3.0-SNAPSHOT";

  private static ArrayList<ProbeHandlerPluginIntf> handlers      = new ArrayList<ProbeHandlerPluginIntf>();

  NetworkInterface                                 ni            = null;
  protected MulticastSocket                        inboundSocket = null;
  protected InetAddress                            maddress;

  protected CloseableHttpClient                    httpClient;

  private ResponderCLIValues                       cliValues;

  private static class ResponderCLIValues {
    public ResponderCLIValues(ResponderConfigurationBean propsConfig) {
      this.config = propsConfig;
    }

    public ResponderConfigurationBean config = new ResponderConfigurationBean();
  }

  private static class ResponderConfigurationBean {

    public int                         multicastPort;
    public String                      multicastAddress;
    public boolean                     noBrowser         = false;
    public ArrayList<AppHandlerConfig> appHandlerConfigs = new ArrayList<AppHandlerConfig>();
    public String                      networkInterface;

  }

  private static class AppHandlerConfig {
    public String classname;
    public String configFilename;
  }

  public static class ResponderShutdown extends Thread {
    Responder agent;

    public ResponderShutdown(Responder agent) {
      this.agent = agent;
    }

    /**
     * Runs the shutdown hook. Closes any open ports before quitting the main
     * java process thread. This ostensibly avoids leaving dangling resources.
     */
    public void run() {
      LOGGER.info("Responder shutting down port "
          + agent.cliValues.config.multicastPort);
      if (agent.inboundSocket != null) {
        try {
          agent.inboundSocket.leaveGroup(agent.maddress);
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, "Error leaving multicast group", e);
        }
        agent.inboundSocket.close();

      }
    }
  }

  public Responder(ResponderCLIValues cliValues) {
    this.cliValues = cliValues;
  }

  public static ArrayList<ProbeHandlerPluginIntf> getHandlers() {
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
   * @throws IOException if there is some issues with the config file
   * @throws ClassNotFoundException if the FQCN is invalid
   */
  public static void addHandler(String classname, String configFilename) throws IOException, ClassNotFoundException {

    ClassLoader cl = ClassLoader.getSystemClassLoader();
    Class<?> handlerClass = cl.loadClass(classname);
    ProbeHandlerPluginIntf handler;

    try {
      handler = (ProbeHandlerPluginIntf) handlerClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      LOGGER.warning("Could not create an instance of the configured handler class - " + classname);
      LOGGER.warning("Using default handler");
      LOGGER.fine("The issue was:");
      LOGGER.fine(e.getMessage());
      handler = new ConfigFileProbeHandlerPluginImpl();
    }

    handler.initializeWithPropertiesFilename(configFilename);

    handlers.add(handler);
  }

  public static void addHandler(ProbeHandlerPluginIntf plugin) {
    handlers.add(plugin);
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
  boolean joinGroup() {
    boolean success = true;
    InetSocketAddress socketAddress = new InetSocketAddress(
        cliValues.config.multicastAddress,
        cliValues.config.multicastPort);
    try {
      // Setup for incoming multicast requests
      maddress = InetAddress.getByName(cliValues.config.multicastAddress);

      if (cliValues.config.networkInterface != null) {
        ni = NetworkInterface.getByName(cliValues.config.networkInterface);
      }
      if (ni == null) {
        InetAddress localhost = InetAddress.getLocalHost();
        LOGGER.fine("Network Interface name not specified.  Using the NI for localhost " + localhost.getHostAddress());
        ni = NetworkInterface.getByInetAddress(localhost);
      }

      LOGGER.info("Starting Responder:  Receiving mulitcast @ "
          + cliValues.config.multicastAddress + ":"
          + cliValues.config.multicastPort);
      this.inboundSocket = new MulticastSocket(
          cliValues.config.multicastPort);

      if (ni == null) { // for some reason NI is still NULL. Not sure why
        // this happens.
        this.inboundSocket.joinGroup(maddress);
        LOGGER
            .warning("Unable to determine the network interface for the localhost address.  Check /etc/hosts for weird entry like 127.0.1.1 mapped to DNS name.");
        LOGGER.info("Unknown network interface joined group "
            + socketAddress.toString());
      } else {
        this.inboundSocket.joinGroup(socketAddress, ni);
        LOGGER.info(ni.getName() + " joined group "
            + socketAddress.toString());
      }
    } catch (IOException e) {
      if (ni == null) {
        LOGGER.log(Level.SEVERE,
            "Error attempting to joint multicast address: ", e);
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

        LOGGER.severe(ni.getName() + " " + buf.toString()
            + ": could not join group " + socketAddress.toString()
            + " --> " + e.toString());
      }
      success = false;
    }
    return success;
  }

  /**
   * This is the main run method for the Argo Responder. It starts up all the
   * necessary machinery and enters the UDP receive loop.
   * 
   * @throws IOException if bad things happen with the configuration files
   * @throws ClassNotFoundException if the classnames for the probe handlers are
   *           bad (usually a type or classpath issue)
   */
  public void run() throws IOException, ClassNotFoundException {

    // load up the handler classes specified in the configuration parameters
    // I hope the hander classes are in a jar file on the classpath
    loadHandlerPlugins(cliValues.config.appHandlerConfigs);

    if (!joinGroup()) {
      LOGGER.severe("Responder shutting down: unable to join multicast group");
      return;
    }

    DatagramPacket packet;
    LOGGER.info("Responder started on " + cliValues.config.multicastAddress
        + ":" + cliValues.config.multicastPort);

    httpClient = HttpClients.createDefault();

    LOGGER.fine("Starting Responder loop - infinite until process terminated");
    // infinite loop until the responder is terminated
    while (true) {

      byte[] buf = new byte[1024];
      packet = new DatagramPacket(buf, buf.length);
      LOGGER.fine("Waiting to recieve packet...");
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
        new ProbeHandlerThread(handlers, probe, httpClient, cliValues.config.noBrowser).start();
      } catch (ProbeParseException e) {
        LOGGER.log(Level.SEVERE, "Error parsing inbound probe payload.", e);
      }

    }

  }

  /**
   * Main entry point for Argo Responder.
   * 
   * @param args command line arguments
   * @throws IOException if bad things happen with the configuration files
   * @throws ClassNotFoundException if the classnames for the probe handlers are
   *           bad (usually a type or classpath issue)
   */
  public static void main(String[] args) throws IOException,
      ClassNotFoundException {

    LOGGER.info("Starting Argo Responder daemon process.");

    readVersionProperties();

    ResponderCLIValues cliValues = parseCommandLine(args);

    if (cliValues == null)
      return;

    Responder responder = new Responder(cliValues);

    LOGGER.info("Responder registering shutdown hook.");
    Runtime.getRuntime().addShutdownHook(new ResponderShutdown(responder));

    responder.run();

  }

  private static void readVersionProperties() {
    InputStream is = Responder.class
        .getResourceAsStream("/version.properties");
    if (is != null) {
      Properties p = new Properties();
      try {
        p.load(is);
        ARGO_VERSION = p.getProperty("argo-version");
      } catch (IOException e) {
        LOGGER.warning("Cannot load the version.properties file");
      }

    } else {
      LOGGER.warning("Cannot open the version.properties file");
    }
  }

  private static ResponderCLIValues parseCommandLine(String[] args) {
    CommandLineParser parser = new BasicParser();
    ResponderCLIValues cliValues = null;

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
    } catch (ResponderConfigException e) {
      LOGGER.log(Level.SEVERE, "Error in Responder configuration.", e);
    }

    return cliValues;
  }

  private static ResponderCLIValues processCommandLine(CommandLine cl)
      throws ResponderConfigException {

    LOGGER.fine("Parsing command line values:");

    ResponderConfigurationBean propsConfig = new ResponderConfigurationBean();
    ResponderCLIValues cliValues = new ResponderCLIValues(propsConfig);

    if (cl.hasOption("pf")) {
      String propsFilename = cl.getOptionValue("pf");
      try {
        propsConfig = processPropertiesValue(propsFilename, propsConfig);
      } catch (Exception e) {
        LOGGER.warning("Unable to read properties file named " + propsFilename + " due to " + e.toString() + " ");
      }
    } else {
      LOGGER.warning("WARNING: no propoerties file specified.  Working off cli override arguments.");
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
        throw new ResponderConfigException("The multicast port number - " + cl.getOptionValue("mp") + " - is not formattable as an integer", e);
      }
    }

    if (cl.hasOption("ma")) {
      propsConfig.multicastAddress = cl.getOptionValue("ma");
      LOGGER.info("Overriding multicast address with command line value");
    }

    return cliValues;

  }

  private void loadHandlerPlugins(ArrayList<AppHandlerConfig> configs) throws IOException, ClassNotFoundException {

    for (AppHandlerConfig appConfig : configs) {
      addHandler(appConfig.classname, appConfig.configFilename);
    }

  }

  private static ResponderConfigurationBean processPropertiesValue(String propertiesFilename, ResponderConfigurationBean config) throws ResponderConfigException {
    Properties prop = new Properties();

    try {
      prop.load(new FileInputStream(propertiesFilename));
    } catch (FileNotFoundException e) {
      throw new ResponderConfigException("Properties file exception:", e);
    } catch (IOException e) {
      throw new ResponderConfigException("Properties file exception:", e);
    }

    try {
      int port = Integer.parseInt(prop.getProperty("multicastPort", "4003"));
      config.multicastPort = port;
    } catch (NumberFormatException e) {
      LOGGER.warning("Error reading port number from properties file.  Using default port of 4003.");
      config.multicastPort = 4003;
    }

    config.multicastAddress = prop.getProperty("multicastAddress");

    // handle the list of appHandler information

    boolean continueProcessing = true;
    int number = 1;
    while (continueProcessing) {
      String appHandlerClassname;
      String configFilename;

      appHandlerClassname = prop.getProperty("probeHandlerClassname." + number, "ws.argo.responder.plugin.ConfigFileProbeHandlerPluginImpl");
      configFilename = prop.getProperty("probeHandlerConfigFilename." + number, null);

      if (configFilename != null) {
        AppHandlerConfig handlerConfig = new AppHandlerConfig();
        handlerConfig.classname = appHandlerClassname;
        handlerConfig.configFilename = configFilename;
        config.appHandlerConfigs.add(handlerConfig);
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
    options.addOption("v", false,
        "display version for the Responder daemon");
    options.addOption(OptionBuilder.withArgName("networkInterface name")
        .hasArg()
        .withDescription("network interface name to listen on")
        .create("ni"));
    options.addOption(OptionBuilder.withArgName("properties filename")
        .hasArg().withType(new String())
        .withDescription("fully qualified properties filename")
        .create("pf"));
    options.addOption(OptionBuilder.withArgName("multicastPort").hasArg()
        .withType(new Integer(0))
        .withDescription("the multicast port to broadcast on")
        .create("mp"));
    options.addOption(OptionBuilder.withArgName("multicastAddr").hasArg()
        .withDescription("the multicast group address to broadcast on")
        .create("ma"));
    options.addOption(OptionBuilder
        .withDescription("setting this switch will disable the responder from returnin all services to a naked probe")
        .create("nb"));

    return options;
  }
}
