package ws.argo.CLClient.commands;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import net.dharwin.common.tools.cli.api.CLIContext;
import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;

@CLICommand(name = "ni", description = "control the network intefaces used to send probes.")
public class NetworkIntefaceCommand extends Command<ArgoClientContext> {

  @Parameter
  List<String> params = new ArrayList<String>();

  @Parameters(commandNames = { "list" }, commandDescription = "list configured network interfaces.")
  public class NIListCommand extends Command<ArgoClientContext> {

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {
      
      StringBuffer buf = new StringBuffer();
      buf.append("Now using the following Network Interfaces: ");
      for (String niName : context.getNIList() ) {
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
      try {
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

        Console.info("Available Multicast-enabled Network Interfaces");
        while (nis.hasMoreElements()) {
          NetworkInterface ni = nis.nextElement();
          if (ni.supportsMulticast()) {
            StringBuffer buf = new StringBuffer();
            buf.append("NI named " + ni.getName());
            if (context.getNIList().contains(ni.getName())) {
              buf.append(" (USING) ");
            }
            buf.append(" at addresses " + ni.getInterfaceAddresses());
            Console.info(buf.toString());
          }
        }

      } catch (SocketException e) {
        Console.error("Error getting Network Interfaces from VM.");
        Console.error(e.getMessage());
        return CommandResult.ERROR;
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
        if (ni != null) {
          niList.add(ni.getName());
        }
      } catch (SocketException | UnknownHostException e) {
        Console.severe("Cannot get the Network Interface for localhost");
        Console.severe(e.getMessage());
      }

      context.setNIList(niList);

      return CommandResult.OK;

    }
  }

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {
    JCommander jc = new JCommander();

    HashMap<String, Command<ArgoClientContext>> commandInstances = getCommands();
    for (Command<ArgoClientContext> commandInstance : commandInstances.values()) {
      jc.addCommand(commandInstance);
    }

    jc.parse(params.toArray(new String[params.size()]));

    String parsedCommand = jc.getParsedCommand();

    Command<ArgoClientContext> command = commandInstances.get(parsedCommand);

    if (command != null) {
      command.execute(context);
    } else {
      Console.info("Unknown ni command: " + jc.getParsedCommand());
    }

    return CommandResult.OK;
  }

  @SuppressWarnings("unchecked")
  private HashMap<String, Command<ArgoClientContext>> getCommands() {
    HashMap<String, Command<ArgoClientContext>> commands = new HashMap<String, Command<ArgoClientContext>>();

    Class<? extends Command<? extends CLIContext>>[] commandClasses = (Class<? extends Command<? extends CLIContext>>[]) this.getClass().getDeclaredClasses();

    for (Class<? extends Command<? extends CLIContext>> commandClass : commandClasses) {

      try {

        Constructor<? extends Command<? extends CLIContext>> commandConst = commandClass.getDeclaredConstructor(new Class[] { this.getClass() });

        Command<? extends CLIContext> command = commandConst.newInstance(new Object[] { this });

        String commandName = command.getClass().getAnnotation(Parameters.class).commandNames()[0];

        commands.put(commandName, (Command<ArgoClientContext>) command);

      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
        Console.error("Error instantiating compond command class [" + commandClass.getName() + "]");
        Console.error(e.getLocalizedMessage());
      }

    }

    return commands;
  }

  /**
   * Print the usage for the command. By default, this prints the description
   * and available parameters.
   */
  public void usage() {
    CLICommand commandAnnotation = this.getClass().getAnnotation(CLICommand.class);
    Console.info("Help for [" + commandAnnotation.name() + "].");
    String description = commandAnnotation.description();
    if (description != null && !description.isEmpty()) {
      Console.info("Description: " + description);
    }
    JCommander comm = new JCommander(this);
    comm.setProgramName(commandAnnotation.name());

    HashMap<String, Command<ArgoClientContext>> commandInstances = getCommands();
    for (Command<ArgoClientContext> commandInstance : commandInstances.values()) {
      comm.addCommand(commandInstance);
    }

    comm.usage();
  }

}
