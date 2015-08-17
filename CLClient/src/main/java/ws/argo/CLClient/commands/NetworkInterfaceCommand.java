package ws.argo.CLClient.commands;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import net.dharwin.common.tools.cli.api.CLIContext;
import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.CompoundCommand;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;
import ws.argo.probe.ProbeGenerator;

/**
 * 
 * @author jmsimpson
 *
 */
@CLICommand(name = "ni", description = "control the network intefaces used to send probes.")
public class NetworkInterfaceCommand extends CompoundCommand<ArgoClientContext> {

  @Parameter
  List<String> params = new ArrayList<String>();

  @Parameters(commandNames = { "list" }, commandDescription = "list configured network interfaces.")
  public class NIListCommand extends Command<ArgoClientContext> {

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      StringBuffer buf = new StringBuffer();
      buf.append("Now using the following Network Interfaces: ");
      for (String niName : context.getNIList()) {
        buf.append("[" + niName + "] ");
      }

      Console.info(buf.toString());
      return CommandResult.OK;
    }

  }

  @Parameters(commandNames = { "avail" }, commandDescription = "show available multicast network interfaces.")
  public class NIAvailableCommand extends Command<ArgoClientContext> {

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

  @Parameters(commandNames = { "use" }, commandDescription = "use named multicast network interfaces.")
  public class NIUseCommand extends Command<ArgoClientContext> {

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

  @Parameters(commandNames = { "clear" }, commandDescription = "clear all multicast network interfaces (except localhost NI).")
  public class NIClearCommand extends Command<ArgoClientContext> {

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

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
