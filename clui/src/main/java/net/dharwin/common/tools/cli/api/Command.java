package net.dharwin.common.tools.cli.api;

import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;

import com.beust.jcommander.JCommander;

/**
 * The Command interface contains a command name, which is matched against the
 * user's input, and it contains a method for command execution.
 * After implementing from this interface, the implementing class should
 * use the @CLICommand annotation to mark it as eligible for use by the API.
 * 
 * Note that these command instances should be as light weight as possible, as
 * they are re-created every time the command is invoked. This also means that you should
 * not rely on the state of a Command, such as field members of the command. The next time the
 * command is run, all fields will be reset. If you want to persist something like this,
 * you should use the CLIContext.
 * 
 * To specify command parameters, you should annotate the fields of the command implementation. The annotations
 * should be @Parameter, defined by JCommander. More info is at <a href="http://jcommander.org/">http://jcommander.org/</a>
 * and at <a href="http://code.google.com/p/java-cli-api/wiki/SpecifyingCommandParameters">http://code.google.com/p/java-cli-api/wiki/SpecifyingCommandParameters</a>.
 * 
 * @author Sean
 *
 */
public abstract class Command<T extends CLIContext> {
	
	/**
	 * Executes the command. The command line arguments contains all of
	 * the key-value pairs and switches, but does not include the command name
	 * that came from the original arguments.
	 * @param args The command line arguments.
	 * @return The result of the execution of this command.
	 */
	public CommandResult execute(T context) {
		try {
			return innerExecute(context);
		}
		catch (Exception e) {
			Console.error(e.getMessage());
			return CommandResult.ERROR;
		}
	}
	
	/**
	 * Print the usage for the command.
	 * By default, this prints the description and available parameters.
	 */
	public void usage() {
		CLICommand commandAnnotation = this.getClass().getAnnotation(CLICommand.class);
		Console.info("Help for ["+commandAnnotation.name()+"].");
		String description = commandAnnotation.description();
		if (description != null && !description.isEmpty()) {
			Console.info("Description: " + description);
		}
		JCommander comm = new JCommander(this);
		comm.setProgramName(commandAnnotation.name());
		comm.usage();
	}
	
	/**
	 * Execute the command.
	 * @param context The context in which the command is being executed.
	 * @param args The command line arguments.
	 * @return The command result.
	 */
	protected abstract CommandResult innerExecute(T context);
}
