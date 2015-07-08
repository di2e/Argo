package net.dharwin.common.tools.cli.api.exceptions;

/**
 * An exception representing a failure to initialize the CLI.
 * @author Sean
 *
 */
public class CLIInitException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 182094339183402432L;
	
	public CLIInitException(String message) {
		super(message);
	}
	
	public CLIInitException(String message, Throwable cause) {
		super(message, cause);
	}
}
