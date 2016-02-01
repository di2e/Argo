package ws.argo.CLClient.commands;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.CompoundCommand;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;
import ws.argo.CLClient.ClientProbeSenders;
import ws.argo.plugin.transport.exception.TransportConfigException;

/**
 * This Config command is used to configure various things including the probe
 * senders and associated transports. I'd rather do this differently (with
 * pluggable and configurable transports) but that is a bridge too far for this
 * release. I'll hardcode in new transports as I go for now.
 * 
 * @author jmsimpson
 *
 */
@CLICommand(name = "config", description = "manage configuration")
public class ConfigCommand extends CompoundCommand<ArgoClientContext> {

  @Parameter(names = "-defaultCID", description = "set the default cid used in new probes")
  private String _defaultCID;

  @Parameter(names = { "-url", "--listenerURL" }, description = "URL for the client response listener.  Setting this will automatically restart the listener.")
  private String _url;

  @Parameter(names = { "-rurl", "--respondToURL" }, description = "respondTo URL to use for the probes.")
  private String _rurl;

  /**
   * Manage the configuration of the probe transports.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "transport" }, commandDescription = "manage the transports")
  public class Transport extends CompoundCommand<ArgoClientContext> {

    /**
     * List the probe transports that are currently active.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "show" }, commandDescription = "shows the configuration of the named transports.")
    public class Show extends Command<ArgoClientContext> {

      @Parameter(names = { "-n", "--name" }, description = "name of the transport.", required = true)
      private String _transportName;

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        ClientProbeSenders transport = context.getClientTransportNamed(_transportName);
        if (transport != null) {
          Console.info(transport.showConfiguration());
        } else {
          Console.error("No transport named [" + _transportName + "]");
        }
        return CommandResult.OK;
      }

    }

    /**
     * List the probe transports that are currently active.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "list" }, commandDescription = "list the currently setup probe transports.")
    public class List extends Command<ArgoClientContext> {

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        ArrayList<ClientProbeSenders> gens = context.getClientTransports();

        for (ClientProbeSenders t : gens) {
          Console.info(t.getDescription());
        }

        return CommandResult.OK;
      }

    }

    /**
     * List the probe transports that are currently active.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "enable" }, commandDescription = "enables a transport with the given name.")
    public class Enable extends Command<ArgoClientContext> {

      @Parameter(names = { "-n", "--name" }, description = "name of the transport.", required = true)
      private String _transportName;

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        ClientProbeSenders transport = context.getClientTransportNamed(_transportName);

        if (transport != null) {
          transport.setEnabled(true);
          Console.info("Enabled transport named [" + _transportName + "]");
        } else {
          Console.error("No transport named [" + _transportName + "]");
        }

        return CommandResult.OK;
      }

    }

    /**
     * List the probe transports that are currently active.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "disable" }, commandDescription = "disables a transport with the given name.")
    public class Disable extends Command<ArgoClientContext> {

      @Parameter(names = { "-n", "--name" }, description = "name of the transport.", required = true)
      private String _transportName;

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        ClientProbeSenders transport = context.getClientTransportNamed(_transportName);

        if (transport != null) {
          transport.setEnabled(false);
          Console.info("Disabled transport named [" + _transportName + "]");
        } else {
          Console.error("No transport named [" + _transportName + "]");
        }
        return CommandResult.OK;
      }

    }

    /**
     * List the probe transports that are currently active.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "reload" }, commandDescription = "reloads a transport with the given name.")
    public class Reload extends Command<ArgoClientContext> {

      @Parameter(names = { "-n", "--name" }, description = "name of the transport.", required = true)
      private String _transportName;

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        return CommandResult.OK;
      }

    }

    /**
     * List the probe transports that are currently active.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "restart" }, commandDescription = "restart a transport with the given name.")
    public class Restart extends Command<ArgoClientContext> {

      @Parameter(names = { "-n", "--name" }, description = "name of the transport.", required = false)
      private String _transportName;
      
      private boolean _all = false;

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        if (_transportName == null || _transportName.isEmpty())
          _all = true;
        
        for (ClientProbeSenders senders : context.getClientTransports()) {
          if (_all || (_transportName != null && _transportName.equalsIgnoreCase(senders.getName()))) {
            try {
              Console.info("Restarting the Client Probe Sender [" + senders.getName() + "].");
              senders.close();
              senders.restart(context);
              Console.info("Successfully restarted [" + senders.getName() + "].");
            } catch (TransportConfigException e) {
              Console.error(e.getLocalizedMessage());
            }
          }
        }
        
        return CommandResult.OK;
      }

    }

  }

  /**
   * Configure the Network Interfaces. This really is only for the MC transport
   * at the moment.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "ni" }, commandDescription = "manage network interfaces for multicast probes")
  public class NI extends CompoundCommand<ArgoClientContext> {

    /**
     * List the available network interfaces the client can use.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "avail" }, commandDescription = "show available network interfaces.")
    public class Available extends Command<ArgoClientContext> {

      @Parameter(names = { "-mc", "--multicast" }, description = "show only multicast enabled")
      private boolean _mcEnabled;

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        List<String> niNames;
        try {
          niNames = context.getAvailableNetworkInterfaces(_mcEnabled);
        } catch (SocketException e1) {
          Console.error("Issues getting the available network interfaces.");
          Console.error(e1.getMessage());
          return CommandResult.ERROR;
        }

        if (_mcEnabled) {
          Console.info("Available Multicast enabled Network Interfaces");
        } else {
          Console.info("All Available Network Interfaces");
        }

        for (String niName : niNames) {
          try {
            NetworkInterface ni = NetworkInterface.getByName(niName);
            StringBuffer buf = new StringBuffer();
            buf.append("NI named " + ni.getName());
            if (context.getNIList().contains(ni.getName())) {
              buf.append(" (USING) ");
            }
            buf.append(" at addresses " + ni.getInterfaceAddresses());
            Console.info(buf.toString());
          } catch (SocketException e) {
            Console.error("Issues getting the network interface for name [" + niName + "]");
            Console.error(e.getMessage());
          }
        }

        return CommandResult.OK;
      }

    }

    /**
     * Enable or use the specified NI. This will tell the
     * {@linkplain ClientProbeSenders#getSenders()} call to return the
     * pre-created MC probe sender in the list of ProbeSender to use when
     * sending a probe.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "use" }, commandDescription = "use named multicast network interfaces.")
    public class Use extends Command<ArgoClientContext> {

      @Parameter
      public List<String> _niNames = new ArrayList<>();

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        if (!_niNames.isEmpty()) {
          for (String niName : _niNames) {
            NetworkInterface ni = null;
            try {
              ni = NetworkInterface.getByName(niName);
            } catch (SocketException e) {
              Console.error("SocketException when attempting to get Network Interface named [" + niName + "]");
              Console.error(e.getMessage());
            }
            if (ni == null) {
              Console.error("Network Interface named [" + niName + "] does not exist.");
            } else {
              if (!context.getNIList().contains(niName)) {
                context.getNIList().add(niName);
                Console.info("Added Network Interface named [" + niName + "]");
              } else {
                Console.info("Already using Network Interface named [" + niName + "]");
              }
            }
          }
        }

        return CommandResult.OK;

      }
    }

    /**
     * Disables or ignore a network interface for the multicast transport.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "ignore" }, commandDescription = "ignore named multicast network interfaces (opposite of use).")
    public class Ignore extends Command<ArgoClientContext> {

      @Parameter
      public List<String> _niNames = new ArrayList<>();

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        if (!_niNames.isEmpty()) {
          for (String niName : _niNames) {
            NetworkInterface ni = null;
            try {
              ni = NetworkInterface.getByName(niName);
            } catch (SocketException e) {
              Console.error("SocketException when attempting to get Network Interface named [" + niName + "]");
              Console.error(e.getMessage());
            }
            if (ni == null) {
              Console.error("Network Interface named [" + niName + "] does not exist.");
            } else {
              if (context.getNIList().contains(niName)) {
                context.getNIList().remove(niName);
                Console.info("Ignoring Network Interface named [" + niName + "]");
                if (context.getNIList().isEmpty()) {
                  Console.info("ATTENTION:  You are not using any available Network Interfaces to send probes that require a NI to work.");
                  Console.info("ATTENTION:  You must use at least one NI to send probes for those type of transports.");
                  Console.info("ATTENTION:  Use 'config transport list' to see which transports require a NI.");
                }
              } else {
                Console.info("Already ignoring Network Interface named [" + niName + "]");
              }
            }
          }
        }

        return CommandResult.OK;

      }
    }

    /**
     * The Clear command clears out the multicast network interfaces.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "clear" }, commandDescription = "clear all multicast network interfaces (except localhost NI).")
    public class Clear extends Command<ArgoClientContext> {

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        context.resetNIList();

        return CommandResult.OK;

      }
    }
  }

  /**
   * The restart command explicitly restarts the client listener if the user makes certain changes
   * to the security key store parameters.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "restart" }, commandDescription = "restart the listener.")
  public class Restart extends Command<ArgoClientContext> {

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      context.restartListener(context.getListenerURL());

      return CommandResult.OK;
    }

  }
  
  @Parameters(commandNames = { "keystores" }, commandDescription = "manages the keystore information.")
  public class Keystores extends Command<ArgoClientContext> {

    @Parameter(names = {"-ks", "--keystoreFilename"}, description = "the keystore filename")
    private String _keystoreFilename;
    @Parameter(names = {"-kspw", "--keystorePassword"}, description = "the keystore password")
    private String _keystorePassword;
    @Parameter(names = {"-ts", "--truststoreFilename"}, description = "the truststore filename")
    private String _truststoreFilename;
    @Parameter(names = {"-tspw", "--truststorePassword"}, description = "the truststore password")
    private String _truststorePassword;
    
    @Parameter(names = {"-v", "--validate"}, description = "the truststore password")
    private Boolean _validate = false;
  

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {
      
      if (_keystoreFilename != null) {
        context.getConfig().setKeystore(_keystoreFilename);
      }
      
      if (_keystorePassword != null) {
        context.getConfig().setKSPassword(_keystorePassword);
      }

      if (_truststoreFilename != null) {
        context.getConfig().setTruststore(_truststoreFilename);
      }

      if (_truststorePassword != null) {
        context.getConfig().setTSPassword(_truststorePassword);
      }
      
      if (_validate) {
        SSLContextConfigurator sslContext = new SSLContextConfigurator();
        
        // set up security context
        sslContext.setKeyStoreFile(context.getConfig().getKeystore()); // contains listener self-signed certificate
        sslContext.setKeyStorePass(context.getConfig().getKSPassword());
        sslContext.setTrustStoreFile(context.getConfig().getTruststore()); // contains listener self-signed certificate
        sslContext.setTrustStorePass(context.getConfig().getTSPassword());
        
        Logger logger = Grizzly.logger(SSLContextConfigurator.class);
        Level level = logger.getLevel();
        final ConsoleHandler handler = new ConsoleHandler();
        logger.setLevel(Level.ALL);
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        
        if (!sslContext.validateConfiguration(true)) {
          Console.warn("The SSL Context is not valid. To see details, set the logging level to FINE.");
          Console.warn("The client listener MAY NOT WORK PROPERLY.  Check the log and adjust accordingly.");
        } else {
          Console.info("The keystore configuration is valid.");
        }
        logger.setLevel(level);
      }

      return CommandResult.OK;
    }

  }
  
  /**
   * The show command shows the current configuration.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "show" }, commandDescription = "show the current configuration")
  public class Show extends Command<ArgoClientContext> {

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      Console.info("\n------------------ Client URL Information --------------------");
      Console.info("  Client Listener URL ...... " + context.getListenerURL());
      Console.info("    (use 'config -url' to change.  Setting this will restart the listener)");
      Console.info("  Client RespondTo URL ..... " + context.getRespondToURL());
      Console.info("    (use 'config -rurl' to change. All subsequest probes will use that URL)");
      Console.info("  Default CID ... " + context.getDefaultCID());
      Console.info("    (use 'config -defaultCID' to change)");

      Console.info("\n------------------ Client Keystore Information --------------------");
      Console.info("    Keystore Filename: " + context.getConfig().getKeystore()); 
      Console.info("    Keystore Password: " + context.getConfig().getKSPassword()); 
      Console.info("  Truststore Filename: " + context.getConfig().getTruststore()); 
      Console.info("  Truststore Password: " + context.getConfig().getTSPassword()); 
      
      Console.info("\n------------------ Configured Transports --------------------");

      List<ClientProbeSenders> transports = context.getClientTransports();

      for (ClientProbeSenders t : transports) {
        Console.info(t.showConfiguration());
      }

      return CommandResult.OK;
    }

  }

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {

    if (_defaultCID != null) {
      Console.info("Setting default CID to: " + _defaultCID);
      context.setDefaultCID(_defaultCID);
    }

    if (_url != null) {
      context.restartListener(_url);
    }

    if (_rurl != null) {
      context.getConfig().setResponseURL(_rurl);
    }

    return CommandResult.OK;
  }

}
