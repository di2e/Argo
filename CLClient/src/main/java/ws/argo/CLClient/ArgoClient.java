package ws.argo.CLClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Scanner;
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
import org.apache.commons.configuration.ConfigurationException;
import org.glassfish.grizzly.http.server.HttpServer;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import net.dharwin.common.tools.cli.api.CLIContext;
import net.dharwin.common.tools.cli.api.CommandLineApplication;
import net.dharwin.common.tools.cli.api.annotations.CLIEntry;
import net.dharwin.common.tools.cli.api.console.Console;
import net.dharwin.common.tools.cli.api.exceptions.CLIInitException;
import ws.argo.CLClient.config.ClientConfiguration;
import ws.argo.CLClient.listener.CacheListener;
import ws.argo.CLClient.listener.ProbeResponseResource;
import ws.argo.CLClient.listener.ResponseListener;

/**
 * The ArgoClient represents the command line client for sending probes and
 * receiving responses from Argo Responders on the reachable network.
 * 
 * @author jmsimpson
 *
 */
@CLIEntry
public class ArgoClient extends CommandLineApplication<ArgoClientContext> implements CacheListener {

  private static final Logger LOGGER = Logger.getLogger(ArgoClient.class.getName());

  private HttpServer          _server;

  private ClientConfiguration _config;

  public ArgoClient() throws CLIInitException {
    super();
  }

  private void startListener() throws IOException, URISyntaxException {
    String urlString = _config.getListenerURL();
    
    URI listenerURL = ResponseListener.DEFAULT_LISTENER_URI;
    if (urlString != null)
      listenerURL = new URI(urlString); // This should not be malformed as it's
                                        // checked earlier

    _server = ResponseListener.startServer(listenerURL, _config.getSSLContextConfigurator());

    ProbeResponseResource.setCacheUpdateListener(this);
  }

  @Override
  public void initialize(String[] args) throws CLIInitException {
    Console.info("Starting Argo Client ...");
    try {
      _config = parseCommandLine(args);
    } catch (ConfigurationException e) {
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
    if (getConfig().setListenerURL(_url)) {
      if (_server != null)
        _server.shutdownNow();
      try {
        startListener();
      } catch (IOException | URISyntaxException e) {
        Console.severe("Unable to start services.");
        e.printStackTrace();
      }
    }
  }

  private void startServices() throws CLIInitException {
    try {
      startListener();
    } catch (IOException | URISyntaxException e) {
      // Console.severe("Unable to start services.");
      // e.printStackTrace();
      throw new CLIInitException("Unable to start services", e);
    }
  }

  @Override
  protected void shutdown() {
    for (ClientProbeSenders senders : ((ArgoClientContext) _appContext).getClientTransports()) {
      senders.close();
    }
    _server.shutdownNow();
    System.out.println("Shutting down ArgoClient.");
  }

  @Override
  protected CLIContext createContext() {
    return new ArgoClientContext(this);
  }

  public ClientConfiguration getConfig() {
    return _config;
  }

  public ArrayList<TransportConfig> getTransportConfigs() {
    return getConfig().getTransportConfigs();
  }

  private ClientConfiguration parseCommandLine(String[] args) throws ConfigurationException {
    CommandLineParser parser = new BasicParser();
    ClientConfiguration config = null;

    // Process the help option
    try {
      CommandLine cl = parser.parse(getOptions(), args);

      if (cl.hasOption("h")) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Responder", getOptions());
        return null;
      }

      config = processCommandLine(cl);

    } catch (UnrecognizedOptionException e) {
      LOGGER.log(Level.SEVERE, "Error parsing command line:  " + e.getLocalizedMessage());
    } catch (ParseException e) {
      LOGGER.log(Level.SEVERE, "Error parsing option.", e);
    }

    return config;
  }

  private ClientConfiguration processCommandLine(CommandLine cl) throws ConfigurationException {

    LOGGER.fine("Parsing command line values:");

    ClientConfiguration config = new ClientConfiguration(); // default
                                                            // configuration

    if (cl.hasOption("pf")) {
      String propsFilename = cl.getOptionValue("pf");
      try {
        config = processConfigurationFile(propsFilename);
      } catch (ConfigurationException e) {
        LOGGER.log(Level.SEVERE, "Unable to read properties file named [" + propsFilename + "] due to:", e);
        throw e;
      }
    } else {
      LOGGER.warning("WARNING: no properties file specified.  Working off defaults - Multicast transport 230.0.0.1:4003.");
    }

    return config;

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

  private ClientConfiguration processConfigurationFile(String filename) throws ConfigurationException {

    ClientConfiguration config = new ClientConfiguration(filename);

    return config;
  }

  @Override
  public void cacheUpdated(String message) {
    Console.info("\nCACHE UPDATE: " + message);
    if (Boolean.getBoolean("jlineDisable")) {
      System.out.print(_prompt + " (no jline) >");
    } else {
      System.out.print(_prompt + " >");
    }

  }

}
