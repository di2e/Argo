package ws.argo.CLClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
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
import org.apache.commons.validator.routines.UrlValidator;
import org.glassfish.grizzly.http.server.HttpServer;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import net.dharwin.common.tools.cli.api.CLIContext;
import net.dharwin.common.tools.cli.api.CommandLineApplication;
import net.dharwin.common.tools.cli.api.annotations.CLIEntry;
import net.dharwin.common.tools.cli.api.console.Console;
import net.dharwin.common.tools.cli.api.exceptions.CLIInitException;
import ws.argo.CLClient.listener.ResponseListener;
import ws.argo.probe.ProbeGenerator;

/**
 * The ArgoClient represents the command line client for sending probes and
 * receiving responses from Argo Responders on the reachable network.
 * 
 * @author jmsimpson
 *
 */
@CLIEntry
public class ArgoClient extends CommandLineApplication<ArgoClientContext> {

  private static final Logger LOGGER = Logger.getLogger(ProbeGenerator.class.getName());

  static final String DEFAULT_TOPIC_NAME = "arn:aws:sns:us-east-1:627164602268:argoDiscoveryProtocol";

  private HttpServer server;
  
  private Properties _properties;

  public ArgoClient() throws CLIInitException {
    super();
  }

  private void startListener() throws IOException, URISyntaxException {
    String urlString = getProperties().getProperty("url");
    URI listenerURL = ResponseListener.BASE_URI;
    if (urlString != null)
      listenerURL = new URI(urlString); //This should not be malformed as it's checked earlier
    
    server = ResponseListener.startServer(listenerURL);
    WebResource target = Client.create().resource(listenerURL);
    _appContext.put("listener", target);
  }

  
  @Override
  public void initialize(String[] args) throws CLIInitException {
    _properties = parseCommandLine(args);
    setPrompt("Argo");
    super.initialize(args);
    startServices();
  }

  /**
   * 
   * @param _url the new URL for the listener to use
   */
  public void restartListener(String _url) {
    if (server != null)
      server.stop();
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
    server.stop();
    System.out.println("Shutting down ArgoClient.");
  }

  @Override
  protected CLIContext createContext() {
    return new ArgoClientContext(this);
  }
  
  public Properties getProperties() {
    return _properties;
  }
    
    
  private static Properties parseCommandLine(String[] args) {
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
  
  private static Properties processCommandLine(CommandLine cl) {

    LOGGER.fine("Parsing command line values:");

    Properties propsConfig = new Properties();

    
    // Might use a Properties file in the future
    
//    if (cl.hasOption("pf")) {
//      String propsFilename = cl.getOptionValue("pf");
//      try {
//        propsConfig = processPropertiesValue(propsFilename, propsConfig);
//      } catch (ResponderConfigException e) {
//        LOGGER.log(Level.SEVERE, "Unable to read properties file named [" + propsFilename + "] due to:", e);
//        throw e;
//      }
//    } else {
//      LOGGER.warning("WARNING: no properties file specified.  Working off cli override arguments.");
//    }


    // If the options explicitly do not want you to use multicast
    propsConfig.put("useMC", !cl.hasOption("noMC"));

    // If the options explicitly want you to use SNS
    propsConfig.put("useSNS", cl.hasOption("useSNS"));
    
    // Listening URL
    if (cl.hasOption("url")) {
      
      // Sanity check on the respondToURL
      // The requirement for the respondToURL is a REST POST call, so that means
      // only HTTP and HTTPS schemes.
      // Localhost is allowed as well as a valid response destination
      String[] schemes = { "http", "https" };
      UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
      
      String url = cl.getOptionValue("url");
      
      if (!urlValidator.isValid(url)) {
        Console.error("The Response Listener URL specified in the command-line is invalid. Continuing with default.");
      } else {
        propsConfig.put("url", url);
      }
    }
    
    // Subscription URL
    if (cl.hasOption("surl")) {

      // Sanity check on the respondToURL
      // The requirement for the respondToURL is a REST POST call, so that means
      // only HTTP and HTTPS schemes.
      // Localhost is allowed as well as a valid response destination
      String[] schemes = { "http", "https" };
      UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
      
      String url = cl.getOptionValue("surl");
      
      if (!urlValidator.isValid(url)) {
        Console.error("The SNS Subscription URL specified in the command-line is invalid. Continuing with default.");
      } else {
        propsConfig.put("surl", url);
      }
    }
      
      
    // Access Key
    if (cl.hasOption("ak")) {
      propsConfig.put("ak", cl.getOptionValue("ak"));
    } else {
      if (cl.hasOption("useSNS")) 
        throw new RuntimeException("Must have Access Key defined if using SNS.  Use the -ak <accessKey> switch on the command line.");
    }
    
    // Secret Key
    if (cl.hasOption("sk")) {
      propsConfig.put("sk", cl.getOptionValue("sk"));
    } else {
      if (cl.hasOption("useSNS")) 
        throw new RuntimeException("Must have Secret Key defined if using SNS.  Use the -sk <secretKey> switch on the command line.");
    }

    // Secret Key
    if (cl.hasOption("sk")) {
      propsConfig.put("sk", cl.getOptionValue("sk"));
    }

    // ARN
    propsConfig.put("arn", cl.getOptionValue("arn", DEFAULT_TOPIC_NAME));

    // Multicast port
    if (cl.hasOption("mp")) {
      try {
        int portNum = Integer.parseInt(cl.getOptionValue("mp"));
        
        propsConfig.put("mp", portNum);
        LOGGER.info("Overriding multicast port with command line value");
      } catch (NumberFormatException e) {
        throw new RuntimeException("The multicast port number [" + cl.getOptionValue("mp") + "]- is not formattable as an integer", e);
      }
    }

    // Multicast group
    if (cl.hasOption("ma")) {
      propsConfig.put("ma", cl.getOptionValue("ma"));
      LOGGER.info("Overriding multicast address with command line value");
    }

    return propsConfig;

  }

  
  @SuppressWarnings("static-access")
  private static Options getOptions() {
    Options options = new Options();

    options.addOption("h", false, "display help for the Responder daemon");
    options.addOption(OptionBuilder
        .withDescription("do not use the Multicast Transport (in use by default)")
        .create("noMC"));
    options.addOption(OptionBuilder
        .withDescription("use the SNS Transport")
        .create("useSNS"));
    options.addOption(OptionBuilder.withArgName("listenerURL url")
        .hasArg().withType("")
        .withDescription("HTTP Listener URL")
        .create("url"));
    options.addOption(OptionBuilder.withArgName("subscriptionURL url")
        .hasArg().withType("")
        .withDescription("SNS Subscription URL")
        .create("surl"));
    options.addOption(OptionBuilder.withArgName("accessKey key")
        .hasArg().withType("")
        .withDescription("AWS access key")
        .create("ak"));
    options.addOption(OptionBuilder.withArgName("secretKey key").hasArg()
        .withType("")
        .withDescription("AWS access key")
        .create("sk"));
    options.addOption(OptionBuilder.withArgName("awsArgoTopic").hasArg()
        .withDescription("the arn of the Argo SNS topic")
        .create("arn"));
    options.addOption(OptionBuilder.withArgName("multicastPort").hasArg()
        .withType(Integer.valueOf(0))
        .withDescription("the multicast port to broadcast on")
        .create("mp"));
    options.addOption(OptionBuilder.withArgName("multicastAddr").hasArg()
        .withDescription("the multicast group address to broadcast on")
        .create("ma"));

    // Need some more keystore stuff when we do encryption and https for the listener (and maybe for SNS signing stuff)
    
    return options;
  }

}
