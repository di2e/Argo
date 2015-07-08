package net.dharwin.common.tools.cli.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import net.dharwin.common.tools.cli.api.CommandResult.CommandResultType;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import net.dharwin.common.tools.cli.api.console.Console.ConsoleLevel;
import net.dharwin.common.tools.cli.api.exceptions.CLIInitException;
import net.dharwin.common.tools.cli.api.exceptions.CommandInitException;
import net.dharwin.common.tools.cli.api.utils.CLIAnnotationDiscovereryListener;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.impetus.annovention.ClasspathDiscoverer;
import com.impetus.annovention.Discoverer;

/**
 * The CommandLineApplication is the base class for any application wanting
 * to run via the command line. This base class will load the known commands, as well
 * as the default commands (unless the implementing class disables the default commands).
 * Furthermore, this base class handles the input, output and looping of the application.
 * @author Sean
 *
 */
public abstract class CommandLineApplication<T extends CLIContext> {
	
	/** The property key to define the default log level. **/
	private static final String DEFAULT_LOG_LEVEL_KEY = "default_log_level";
	
	/** The property key to define whether or not to print log levels. **/
	private static final String PRINT_LOG_LEVEL_KEY = "print_log_levels";
	
	/** The map of command names to command classes. **/
	protected Map<String, Class<? extends Command<? extends CLIContext>>> _commands;
	
	/** The application context. **/
	protected CLIContext _appContext;
	
	/** The CommandLineParser instance. **/
	private CommandLineParser _clParser;

  private String _prompt;
	
	
	/**
	 * Initialize the application. This loads the known commands.
	 * @throws CLIInitException Thrown when commands fail to properly load.
	 */
	public CommandLineApplication() throws CLIInitException {
		_commands = loadCommands();
		_appContext = createContext();
		_clParser = new CommandLineParserImpl();
	}
	
	public void setPrompt(String prompt) {
	  this._prompt = prompt;
	}
	
	/**
	 * Start the application. This will continuously loop until
	 * the user exits the application.
	 * @param args the java command line arguments
	 */
	public void start(String[] args) {
		setDefaultLogLevel();
		
		/*
		 * JLine doesn't run in Eclipse. To get around this, we allow
		 * a property "jlineDisable" to be specified via VM arguments.
		 * This causes it to use standard scanning of System.in and disables
		 * auto complete functionality.
		 */
		if (Boolean.getBoolean("jlineDisable")) {
			Scanner scan = new Scanner(System.in);
			while (true) {
				System.out.print(_prompt + " (no jline) >");
				String nextLine = scan.nextLine();
				processInputLine(nextLine);
			}
		}
		else {
			try {
				ConsoleReader reader = new ConsoleReader();
				reader.setBellEnabled(_appContext.getBoolean("cliapi.bellenabled", false));
				reader.addCompleter(new StringsCompleter(this.getCommandNames().toArray(new String[0])));
				while (true) {
					String nextLine = reader.readLine(_prompt + " >");
					processInputLine(nextLine);
				}
			}
			catch (IOException e) {
				System.err.println("Error reading from input.");
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void processInputLine(String inputLine) {
		String[] rawArgs = _clParser.parse(inputLine);
		if (rawArgs.length == 0) {
			return;
		}
		String commandName = rawArgs[0];
		Command command = null;
		try {
			command = findAndCreateCommand(commandName.toLowerCase());
		}
		catch (CommandInitException e) {
			Console.error("Unable to init command ["+e.getCommandName()+"].");
			return;
		}
		
		if (command == null) {
			Console.error("Command not recognized: " + commandName);
			return;
		}
		
		/*
		 * I wanted to avoid a special case here in CommandLineApplication...
		 * But here it is anyway. If the user typed a command name followed
		 * by "--help", then the command usage is printed.
		 */
		if (rawArgs.length == 2 && rawArgs[1].equalsIgnoreCase("--help")) {
			command.usage();
			return;
		}
		
		// Hand our command to JCommander to be parsed.
		JCommander commander = new JCommander(command);
		try {
			commander.parse(StringUtils.stripArgs(rawArgs, 1));
		}
		catch (ParameterException e) {
			Console.error("Arguments cannot be parsed: " + e.getMessage());
			command.usage();
			return;
		}
		catch (ArrayIndexOutOfBoundsException e) {
			/*
			 * NOTE: Currently, JCommander throws an ArrayIndexOutOfBoundsException if I pass
			 * in a valid option, but I do not specify a value. For example:
			 * loglevel -l
			 * For now, we'll just catch this and print the following message.
			 */
			Console.error("Error parsing arguments: Did you specify a value after providing an option?");
			command.usage();
			return;
		}
		catch (Exception e) {
			// Catch any funny business.
			Console.error("Unknown error while parsing arguments: " + e.getMessage());
			return;
		}
		
		CommandResult result = command.execute(_appContext);
		
		if (result.getStatusCode() != 0) {
			Console.error("Command returned type ["+
					result.getType().name()+"] with status code ["+result.getStatusCode()+"].");
		}
		
		if (result.getType() == CommandResultType.EXIT) {
			this.shutdown();
			System.exit(0);
		}
	}
	
	/**
	 * Find and create the command instance.
	 * @param commandName The command name.
	 * @return The command, or null if the command name is not recognized.
	 * @throws CommandInitException Thrown when the command is recognized but failed to load.
	 */
	protected Command<? extends CLIContext> findAndCreateCommand(String commandName) throws CommandInitException {
		Class<? extends Command<? extends CLIContext>> commandClass = _commands.get(commandName);
		if (commandClass == null) {
			return null;
		}
		try {
			return (Command<? extends CLIContext>)commandClass.newInstance();
		}
		catch (Exception e) {
			throw new CommandInitException(commandName);
		}
	}
	
	protected void setDefaultLogLevel() {
		String defaultLogLevel = _appContext.getString(DEFAULT_LOG_LEVEL_KEY);
		if (defaultLogLevel != null) {
			try {
				ConsoleLevel l = ConsoleLevel.valueOf(defaultLogLevel);
				Console.setLevel(l);
			}
			catch (IllegalArgumentException e) {
				Console.error("Unknown default log level ["+defaultLogLevel+"].");
			}
		}
		if (_appContext.containsKey(PRINT_LOG_LEVEL_KEY)) {
			Console.setPrintLogLevel(_appContext.getBoolean(PRINT_LOG_LEVEL_KEY));
		}
	}
	
	/**
	 * Load the necessary commands for this application.
	 * @return The map of commands.
	 * @throws CLIInitException Thrown when commands fail to properly load.
	 */
	private Map<String, Class<? extends Command<? extends CLIContext>>> loadCommands() throws CLIInitException{
		Map<String, Class<? extends Command<? extends CLIContext>>> commands =
			new HashMap<String, Class<? extends Command<? extends CLIContext>>>();
		
		Discoverer discoverer = new ClasspathDiscoverer();
		CLIAnnotationDiscovereryListener discoveryListener =
			new CLIAnnotationDiscovereryListener(new String[] {CLICommand.class.getName()});
		discoverer.addAnnotationListener(discoveryListener);
		discoverer.discover(true, true, true, true, true);
		
		loadCommands(commands, discoveryListener.getDiscoveredClasses());
		
		if (commands.isEmpty()) {
			throw new CLIInitException("No commands could be loaded.");
		}
		
		return commands;
	}
	
	private void loadCommands(Map<String, Class<? extends Command<? extends CLIContext>>> out,
			List<String> commandClasses) throws CLIInitException {
		
		for (String commandClassName : commandClasses) {

			try {
				@SuppressWarnings("unchecked")
				Class<? extends Command<? extends CLIContext>> commandClass =
					(Class<? extends Command<? extends CLIContext>>) Class.forName(commandClassName);
				
				if (!Command.class.isAssignableFrom(commandClass)) {
					Console.severe("Command class ["+commandClassName+"] is not of type Command.");
					throw new CLIInitException("Command class ["+commandClassName+"] is not of type Command.");
				}
				
				CLICommand annotation = commandClass.getAnnotation(CLICommand.class);
				
				out.put(annotation.name().toLowerCase(), commandClass);
				Console.superFine("Loaded command ["+annotation.name()+"].");
			}
			catch (ClassNotFoundException e) {
				throw new CLIInitException("Unable to find command class ["+commandClassName+"].");
			}
			catch (Exception e) {
				throw new CLIInitException("Unable to load command class ["+commandClassName+"]: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Create the context. By default, this creates a
	 * standard CLIContext. Implementing classes should override
	 * this to specify a custom context.
	 * This MUST be the same type (or a subclass of) those specified
	 * by the command implementations.
	 * @return The context.
	 */
	protected CLIContext createContext() {
		return new CLIContext(this);
	}
	
	/**
	 * Return the list of commands known to this application.
	 * @return The list of commands known to this application.
	 */
	public Set<String> getCommandNames() {
		return _commands.keySet();
	}
	
	/**
	 * Return the map of command names to their respective classes.
	 * @return The map of command names to their respective classes.
	 */
	public Map<String, Class<? extends Command<? extends CLIContext>>> getCommands() {
		return _commands;
	}
	
	/**
	 * Called when the user has cleanly exited the application.
	 */
	protected abstract void shutdown();
}
