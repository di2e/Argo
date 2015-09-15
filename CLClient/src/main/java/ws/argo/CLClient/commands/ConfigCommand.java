package ws.argo.CLClient.commands;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.validator.routines.UrlValidator;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.CompoundCommand;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClient;
import ws.argo.CLClient.ArgoClientContext;
import ws.argo.probe.ProbeGenerator;

/**
 * This Config command is used to configure various things including the probe
 * generators and associated transports. I'd rather do this differently (with
 * plugable and configurable transports) but that is a bridge too far for this
 * release. I'll hardcode in new transports as I go for now.
 * 
 * @author jmsimpson
 *
 */
@CLICommand(name = "config", description = "manage configuration")
public class ConfigCommand extends CompoundCommand<ArgoClientContext> {

  @Parameter(names = "-defaultCID", description = "set the default cid used in new probes")
  private String _defaultCID;

  @Parameter(names = { "-url", "--listenerURL" }, description = "URL for the client response listener.  Setting this will restart the listener.")
  private String _url;

  /**
   * Manage the configuration of the probe generators.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "gen" }, commandDescription = "manage the generators/transports")
  public class Generator extends CompoundCommand<ArgoClientContext> {

    /**
     * List the probe generator/transports that are currently active.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "list" }, commandDescription = "list the currently setup probe generators.")
    public class List extends Command<ArgoClientContext> {

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        Map<String, ProbeGenerator> gens = context.getProbeGenerators();

        for (String s : gens.keySet()) {
          ProbeGenerator gen = gens.get(s);
          Console.info(s + " : " + gen.getDescription());
        }

        return CommandResult.OK;
      }

    }

    /**
     * Configure and manage the SNS Transport probe generator.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "enableSNS" }, commandDescription = "Enable/configure the SNS probe generator.")
    public class UseSNS extends Command<ArgoClientContext> {

      @Parameter(names = { "-ak", "--accessKey" }, description = "Amazon Access Key")
      private String _ak;

      @Parameter(names = { "-sk", "--secretKey" }, description = "Amazon Secret Key")
      private String _sk;

      @Parameter(names = { "-arn", "--topicARN" }, description = "SNS Topic ARN")
      private String _arn;

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {
        boolean recreateSNSTransport = false;
        
        if (_ak != null) {
          context.setAccessKey(_ak);
          recreateSNSTransport = true;
        }

        if (_sk != null) {
          context.setSecretKey(_sk);
          recreateSNSTransport = true;
        }

        if (_arn != null) {
          context.setSNSTopicARN(_arn);
          recreateSNSTransport = true;
        }

        if (context.getAccessKey() == null || context.getSecretKey() == null) {
          Console.error("Either the Amazon Acccess Key or the Secrey key has not been set.");
          Console.error("Please setup the Amazon keys using the 'enableSNS -ak <key> -sk <key>' command.");
          return CommandResult.ERROR;
        }

        context.setUseSNS(true);
        if (recreateSNSTransport)
          context.initializeSNSProbeGenerator();
        Console.info("Client will now use SNS.");
        return CommandResult.OK;
      }

    }

    /**
     * Configure and manage the Multicast Transport probe generator.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "enableMC" }, commandDescription = "enable the Multicast probe generators.")
    public class UseMC extends Command<ArgoClientContext> {

      @Parameter(names = { "-ma", "--multicastAddress" }, description = "Multicast Address")
      private String _ma;

      @Parameter(names = { "-mp", "--multicastPort" }, description = "Multicast Port")
      private String _mp;

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        if (_ma != null) {
          context.setMulticastAddress(_ma);
        }

        if (_mp != null) {
          context.setMulticastPort(_mp);
        }

        context.setUseMulticast(true);
        Console.info("Client will now use Multicast.");
        return CommandResult.OK;
      }

    }

    /**
     * Disable the SNS transport.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "disableSNS" }, commandDescription = "Do not the SNS probe generator.")
    public class NoSNS extends Command<ArgoClientContext> {

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        context.setUseSNS(false);
        Console.info("Client will not use SNS");
        return CommandResult.OK;
      }

    }

    /**
     * Disable the MC transport.
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "disableMC" }, commandDescription = "Do not the MC probe generators.")
    public class NoMC extends Command<ArgoClientContext> {

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        context.setUseMulticast(false);
        Console.info("Client will not use Multicast");
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
    @Parameters(commandNames = { "avail" }, commandDescription = "show available multicast network interfaces.")
    public class Available extends Command<ArgoClientContext> {

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        Map<String, ProbeGenerator> probeGens = context.getProbeGenerators();

        Console.info("Available Multicast-enabled Network Interfaces");
        for (String niName : probeGens.keySet()) {
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
     * {@linkplain ArgoClientContext#getProbeGenerators()} call to return the
     * pre-created MC probe generator in the list of ProbeGenerator to use when
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

        if (!context.isUseMulticast()) {
          Console.error("Cannot use network interfaces at the moment as multicast is not in use.");
          Console.error("Use 'config useMC' command to start using multicast.");

          return CommandResult.OK;
        }

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

        if (!context.isUseMulticast()) {
          Console.error("Cannot use network interfaces at the moment as multicast is not in use.");
          Console.error("Use 'config useMC' command to start using multicast.");

          return CommandResult.OK;
        }

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
                  Console.info("ATTENTION:  You are not using any available Network Interfaces to send multicast traffic.  You must use at least one NI to send probes.");
                  Console.info("This is the same as 'config noMC'.  Please use an avaialable NI.");
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
     * 
     * @author jmsimpson
     *
     */
    @Parameters(commandNames = { "clear" }, commandDescription = "clear all multicast network interfaces (except localhost NI).")
    public class Clear extends Command<ArgoClientContext> {

      @Override
      protected CommandResult innerExecute(ArgoClientContext context) {

        if (!context.isUseMulticast()) {
          Console.error("Cannot use network interfaces at the moment as multicast is not in use.");
          Console.error("Use 'config useMC' command to start using multicast.");

          return CommandResult.OK;
        }

        InetAddress localhost;
        NetworkInterface ni = null;
        List<String> niList = new ArrayList<String>();
        try {
          localhost = InetAddress.getLocalHost();
          ni = NetworkInterface.getByInetAddress(localhost);

          if (ni != null && context.getProbeGenerators().containsKey(ni.getName())) {
            niList.add(ni.getName());
          } else {
            Console.warn("Unable to get a Probe Generator for NI name [" + ni.getName() + "].");
            String niName = context.getProbeGenerators().keySet().iterator().next();
            Console.warn("Using NI name [" + niName + "] instead.");
            niList.add(niName);
          }

        } catch (SocketException | UnknownHostException e) {
          Console.severe("Cannot get the Network Interface for localhost");
          Console.severe(e.getMessage());
        }

        context.setNIList(niList);

        return CommandResult.OK;

      }
    }
  }

  /**
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "show" }, commandDescription = "show the current configuration")
  public class Show extends Command<ArgoClientContext> {

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      Console.info("-------------------- Basic Information ------------------------");
      Console.info("  Default CID ... " + context.getDefaultCID());
      Console.info("    (use 'config -defaultCID' to change)");

      Console.info("\n------------------ Client URL Information --------------------");
      Console.info("  Client Listener RespondTo URL ... " + context.getURL());
      Console.info("    (use 'config -url' to change.  Setting this will restart the listener)");

      Console.info("\n--------------- Multicast Transport Information ---------------");
      Console.info("  (use config gen enableMC to enable/set MC transport settings)");
      Console.info("  Multicast Enabled ......... " + context.isUseMulticast());
      Console.info("  Multicast Group Address ... " + context.getMulticastAddress());
      Console.info("  Multicast Port ............ " + context.getMulticastPort());
      Console.info("  Network Interfaces ........ " + context.getNIList());
      Console.info("    (use 'config ni avail' to see all available network interfaces)");
      Console.info("    (use 'config ni use' to use a particular network interface)");

      Console.info("\n------------- Amazon SNS Transport Information -----------------");
      Console.info("  SNS Generators Enabled =   " + context.isUseSNS());
      Console.info("  SNS Topic ARN =            " + context.getSNSTopicARN());
      Console.info("  Amazon Access Key =        " + context.getAccessKey());
      Console.info("  Amazon Access Key =        " + context.getSecretKey());
      Console.info("    (use config gen enableSNS to enable/set SNS transport settings)");

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

      // Sanity check on the respondToURL
      // The requirement for the respondToURL is a REST POST call, so that means
      // only HTTP and HTTPS schemes.
      // Localhost is allowed as well as a valid response destination
      String[] schemes = { "http", "https" };
      UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);

      if (!urlValidator.isValid(_url)) {
        Console.error("The Response Listener URL specified is invalid. Not restarting listener.");
        return CommandResult.ERROR;
      } else {
        ((ArgoClient) context.getHostApplication()).restartListener(_url);
      }

    }

    return CommandResult.OK;
  }

}
