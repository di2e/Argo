package net.dharwin.common.tools.cli.api;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;

/**
 * The CompoundCommannd class enables commands that are "sub" commands of
 * others. For example if you had a command called "config" and you wanted to
 * have sub command called "printer" then you would use this class to set that
 * up.
 * 
 * <p>
 * It provides convenience utilities to list the help and usage for a
 * sub-command.
 * 
 * @author jmsimpson
 * @param <T> the class of the context object
 *
 */
public abstract class CompoundCommand<T extends CLIContext> extends Command<T> {

  private HashMap<String, Command<T>> _commands = null;
  
  /**
   * This is not a compound command.
   * 
   * @return is the command compound or not
   */
  public boolean isCompound() {
    return true;
  }
  
  @Override
  public FindCommandResult findAndCreateCommand(String[] args) {
    if (args.length == 0)
      return new FindCommandResult(this, args);
    
    String commandName = args[0];
    String[] remainingArguments = StringUtils.stripArgs(args, 1);
   
    HashMap<String, Command<T>> commandInstances = getCommands();
    
    if (commandInstances.containsKey(commandName)) {
      Command<T> rootCmd = commandInstances.get(commandName);
      return rootCmd.findAndCreateCommand(remainingArguments);
    }
    
    return new FindCommandResult(this, args);
  }

  
  protected CommandResult innerExecute(T context) {
    Console.error("Arguments or subcommand expected.");
    this.usage();
    return CommandResult.OK;
  }

  @SuppressWarnings("unchecked")
  private HashMap<String, Command<T>> getCommands() {
    
    // Really, just do this once
    if (_commands != null)
      return _commands;
    
    HashMap<String, Command<T>> commands = new HashMap<String, Command<T>>();

    Class<? extends Command<? extends CLIContext>>[] commandClasses = (Class<? extends Command<? extends CLIContext>>[]) this.getClass().getDeclaredClasses();

    for (Class<? extends Command<? extends CLIContext>> commandClass : commandClasses) {
      try {
        Constructor<? extends Command<? extends CLIContext>> commandConst = commandClass.getDeclaredConstructor(new Class[] { this.getClass() });
        Command<? extends CLIContext> command = commandConst.newInstance(new Object[] { this });

        String commandName = command.getClass().getAnnotation(Parameters.class).commandNames()[0];

        commands.put(commandName, (Command<T>) command);

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
    String commandName = "";
    String commandDescription = "";
    
    CLICommand commandAnnotation = this.getClass().getAnnotation(CLICommand.class);
    
    if (commandAnnotation == null) {
      Parameters commandParameters = this.getClass().getAnnotation(Parameters.class);
      if (commandParameters != null) {
        commandName = commandParameters.commandNames()[0];
        commandDescription = commandParameters.commandDescription();
      }
    } else {
      commandName = commandAnnotation.name();
      commandDescription = commandAnnotation.description();
    }
    
    Console.info("Help for ["+commandName+"].");
    if (commandDescription != null && !commandDescription.isEmpty()) {
      Console.info("Description: " + commandDescription);
    }
    
    JCommander comm = new JCommander(this);
    comm.setProgramName(commandName);

    HashMap<String, Command<T>> commandInstances = getCommands();
    for (Command<T> commandInstance : commandInstances.values()) {
      comm.addCommand(commandInstance);
    }

    comm.usage();
  }

}
