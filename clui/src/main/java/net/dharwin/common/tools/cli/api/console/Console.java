package net.dharwin.common.tools.cli.api.console;

import java.io.PrintStream;

/**
 * A poor implementation of a watered-down logging/printing api.
 * This basically just handles the printing of messages to System.out/err and
 * suppressing log levels below a threshold.
 * @author Sean
 *
 */
public class Console {
	
	/** The currently set output level. **/
	private static ConsoleLevel _level = ConsoleLevel.INFO;
	
	/** Whether or not to prefix each output message with the log level. **/
	private static boolean _printLogLevel = false;
	
	/**
	 * Set whether or not to print the log level before each message.
	 * @param print flag to print or not
	 */
	public static void setPrintLogLevel(boolean print) {
		_printLogLevel = print;
	}
	
	/**
	 * Set the log level threshold.
	 * @param level logging level
	 */
	public static void setLevel(ConsoleLevel level) {
		_level = level;
	}
	
	/**
	 * Print the given message at the given level.
	 * @param level The level associated with the message.
	 * @param message The message.
	 */
	public static void print(ConsoleLevel level, String message) {
		if (level.compareTo(_level) <= 0) {
			if (_printLogLevel) {
				level.getStream().println(level.name() + ": " + message);
			}
			else {
				level.getStream().println(message);
			}
			level.getStream().flush();
		}
	}
	
	/**
	 * Print the message to the SEVERE level.
	 * @param message The message to print.
	 */
	public static void severe(String message) {
		print(ConsoleLevel.SEVERE, message);
	}
	
	/**
	 * Print the message to the ERROR level.
	 * @param message The message to print.
	 */
	public static void error(String message) {
		print(ConsoleLevel.ERROR, message);
	}
	
	/**
	 * Print the message to the WARN level.
	 * @param message The message to print.
	 */
	public static void warn(String message) {
		print(ConsoleLevel.WARN, message);
	}
	
	/**
	 * Print the message to the INFO level.
	 * @param message The message to print.
	 */
	public static void info(String message) {
		print(ConsoleLevel.INFO, message);
	}
	
	/**
	 * Print the message to the DEBUG level.
	 * @param message The message to print.
	 */
	public static void debug(String message) {
		print(ConsoleLevel.DEBUG, message);
	}
	
	/**
	 * Print the message to the SUPERFINE level.
	 * @param message The message to print.
	 */
	public static void superFine(String message) {
		print(ConsoleLevel.SUPERFINE, message);
	}
	
	
	/**
	 * Console levels in order from most critical to least.
	 * @author Sean
	 *
	 */
	public enum ConsoleLevel {
		SEVERE(System.err),
		ERROR(System.err),
		WARN(System.out),
		INFO(System.out),
		DEBUG(System.out),
		SUPERFINE(System.out);
		
		private PrintStream _stream;
		
		private ConsoleLevel(PrintStream stream) {
			_stream = stream;
		}
		
		public PrintStream getStream() {
			return _stream;
		}
	}
}
