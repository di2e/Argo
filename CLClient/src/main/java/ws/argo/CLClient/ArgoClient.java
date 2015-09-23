package ws.argo.CLClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.validator.routines.UrlValidator;
import org.glassfish.grizzly.http.server.HttpServer;

import net.dharwin.common.tools.cli.api.CLIContext;
import net.dharwin.common.tools.cli.api.CommandLineApplication;
import net.dharwin.common.tools.cli.api.annotations.CLIEntry;
import net.dharwin.common.tools.cli.api.console.Console;
import net.dharwin.common.tools.cli.api.exceptions.CLIInitException;
import ws.argo.CLClient.listener.ResponseListener;

/**
 * The ArgoClient represents the command line client for sending probes and
 * receiving responses from Argo Responders on the reachable network.
 * 
 * @author jmsimpson
 *
 */
@CLIEntry
public class ArgoClient extends CommandLineApplication<ArgoClientContext> {

  private static final Logger LOGGER = Logger.getLogger(ArgoClient.class.getName());

  static final String DEFAULT_TOPIC_NAME    = "arn:aws:sns:us-east-1:627164602268:argoDiscoveryProtocol";
  static final String DEFAULT_LISTENER_HOST = "localhost";

  private HttpServer server;

  private Properties                       _properties;
  private ArrayList<TransportConfig> _transportConfigs = new ArrayList<TransportConfig>();

  public ArgoClient() throws CLIInitException {
    super();
  }

  private void startListener() throws IOException, URISyntaxException {
    String urlString = getProperties().getProperty("url");
    URI listenerURL = ResponseListener.DEFAULT_LISTENER_URI;
    if (urlString != null)
      listenerURL = new URI(urlString); // This should not be malformed as it's
                                        // checked earlier

    server = ResponseListener.startServer(listenerURL);
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(listenerURL);
    _appContext.put("listener", target);
  }

  @Override
  public void initialize(String[] args) throws CLIInitException {
    Console.info("Starting Argo Client ...");
    try {
      _properties = parseCommandLine(args);
    } catch (ArgoClientConfigException e) {
      throw new CLIInitException("Argo Configuration Error", e);
    }
    setPrompt("Argo");
    super.initialize(args);
    startServices();
  }

  /**
   * This will restart the embedded HTTP(S) Grizzly server that acts as the
   * listener for the Probe responses.
   * 
   * @param _url the new URL for the listener to use
   */
  public void restartListener(String _url) {
    if (server != null)
      server.shutdownNow();
    getProperties().put("url", _url);
    try {
      startListener();
    } catch (IOException | URISyntaxException e) {
      Console.severe("Unable to start services.");
      e.printStackTrace();
    }
  }

  private void startServices() {
    try {
      startListener();
    } catch (IOException | URISyntaxException e) {
      Console.severe("Unable to start services.");
      e.printStackTrace();
    }
  }

  @Override
  protected void shutdown() {
    server.shutdownNow();
    System.out.println("Shutting down ArgoClient.");
  }

  @Override
  protected CLIContext createContext() {
    return new ArgoClientContext(this);
  }

  public Properties getProperties() {
    return _properties;
  }

  public ArrayList<TransportConfig> getTransportConfigs() {
    return _transportConfigs;
  }

  private Properties parseCommandLine(String[] args) throws ArgoClientConfigException {
    CommandLineParser parser = new BasicParser();
    Properties cliValues = new Properties();

    // Process the help option
    try {
      CommandLine cl = parser.parse(getOptions(), args);

      if (cl.hasOption("h")) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Responder", getOptions());
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

  private Properties processCommandLine(CommandLine cl) throws ArgoClientConfigException {

    LOGGER.fine("Parsing command line values:");

    Properties propsConfig = new Properties();

    if (cl.hasOption("pf")) {
      String propsFilename = cl.getOptionValue("pf");
      try {
        propsConfig = processPropertiesFile(propsFilename);
      } catch (ArgoClientConfigException e) {
        LOGGER.log(Level.SEVERE, "Unable to read properties file named [" + propsFilename + "] due to:", e);
        throw e;
      }
    } else {
      LOGGER.warning("WARNING: no properties file specified.  Working off defaults - Multicast transport 230.0.0.1:4003.");
    }

    // keeping this around as an example of how to do URL validation.

    /*
     * // Subscription URL if (cl.hasOption("surl")) {
     * 
     * // Sanity check on the respondToURL // The requirement for the
     * respondToURL is a REST POST call, so that means // only HTTP and HTTPS
     * schemes. // Localhost is allowed as well as a valid response destination
     * String[] schemes = { "http", "https" }; UrlValidator urlValidator = new
     * UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
     * 
     * String url = cl.getOptionValue("surl");
     * 
     * if (!urlValidator.isValid(url)) { Console.error(
     * "The SNS Subscription URL specified in the command-line is invalid. Continuing with default."
     * ); } else { propsConfig.put("surl", url); } }
     */

    return propsConfig;

  }

  @SuppressWarnings("static-access")
  private static Options getOptions() {
    Options options = new Options();

    options.addOption("h", false, "display help for the Argo Client");

    options.addOption(OptionBuilder.withArgName("properties filename")
        .hasArg().withType("")
        .withDescription("fully qualified properties filename")
        .create("pf"));

    // Need some more keystore stuff when we do encryption and https for the
    // listener (and maybe for SNS signing stuff)

    return options;
  }

  private Properties processPropertiesFile(String propertiesFilename) throws ArgoClientConfigException {
    Properties prop = new Properties();

    ArrayList<TransportConfig> transportConfigs = getTransportConfigs();

    InputStream is = null;
    try {
      if (ArgoClient.class.getResource(propertiesFilename) != null) {
        is = ArgoClient.class.getResourceAsStream(propertiesFilename);
        LOGGER.info("Reading Argo Client properties file [" + propertiesFilename + "] from classpath.");
      } else {
        is = new FileInputStream(propertiesFilename);
        LOGGER.info("Reading Argo Client properties file [" + propertiesFilename + "] from file system.");
      }
      prop.load(is);
    } catch (FileNotFoundException e) {
      throw new ArgoClientConfigException(e.getLocalizedMessage(), e);
    } catch (IOException e) {
      throw new ArgoClientConfigException(e.getLocalizedMessage(), e);
    } finally {
      try {
        is.close();
      } catch (Exception e) {
        throw new ArgoClientConfigException(e.getLocalizedMessage(), e);
      }
    }
    
    // Listening URL
    
    String listenerURL = prop.getProperty("listenerURL", ResponseListener.DEFAULT_LISTENER_URI.toString());
    
    // Sanity check on the respondToURL
    // The requirement for the respondToURL is a REST POST call, so that means
    // only HTTP and HTTPS schemes.
    // Localhost is allowed as well as a valid response destination
    String[] schemes = { "http", "https" };
    UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
    
    if (!urlValidator.isValid(listenerURL)) {
      listenerURL = ResponseListener.DEFAULT_LISTENER_URI.toString();
      Console.error("The Response Listener URL specified in the config file is invalid. Continuing with default.");
    }
    prop.put("listenerURL", listenerURL);

    // RespondTo URL
    
    String respondToURL = prop.getProperty("respondToURL", listenerURL);
        
    if (!urlValidator.isValid(respondToURL)) {
      respondToURL = listenerURL;
      Console.error("The respondTo URL specified in the config file is invalid. Continuing with default.");
    }
    prop.put("respondToURL", listenerURL);

    // handle the list of transport information

    // You know, this might be better to do as a JSON (or such) file, but you
    // can't comment out lines in JSON

    boolean continueProcessing = true;
    int number = 1;
    while (continueProcessing) {

      String name = prop.getProperty("transportName." + number);
      boolean enabled = Boolean.parseBoolean(prop.getProperty("transportEnabledOnStartup." + number));
      boolean usesNI = Boolean.parseBoolean(prop.getProperty("transportUsesNI." + number));
      boolean requiresMC = Boolean.parseBoolean(prop.getProperty("transportRequiresMulticast." + number));
      String classname = prop.getProperty("transportClassname." + number);
      String configFilename = prop.getProperty("transportConfigFilename." + number, null);

      if (configFilename != null) {
        TransportConfig config = new TransportConfig(name);
        config.setClassname(classname);
        config.setEnabled(enabled);
        config.setUsesNetworkInterface(usesNI);
        config.setRequiresMulticast(requiresMC);
        config.setPropertiesFilename(configFilename);

        transportConfigs.add(config);
      } else {
        continueProcessing = false;
      }
      number++;

    }
    return prop;
  }

}
