package net.dharwin.common.tools.cli.api;

/**
 * A CommandResult specifies the result of a command's execution.
 * It contains a status code and a result type.
 * @author Sean
 *
 */
public class CommandResult {
	
	public static final CommandResult OK = new CommandResult(CommandResultType.STANDARD, 0);
	public static final CommandResult BAD_ARGS = new CommandResult(CommandResultType.BAD_ARGS, 1);
	public static final CommandResult ERROR = new CommandResult(CommandResultType.ERROR, 2);
	
	/** The status of the result. **/
	protected int _statusCode;
	
	/** The type of the result. **/
	protected CommandResultType _type;
	
	/**
	 * Creates a result of type STANDARD.
	 * @param statusCode The status code of the result.
	 */
	public CommandResult(int statusCode) {
		this(CommandResultType.STANDARD, statusCode);
	}
	
	/**
	 * Creates a result with the given type and status code.
	 * @param type The result type.
	 * @param statusCode The status code.
	 */
	public CommandResult(CommandResultType type, int statusCode) {
		_type = type;
		_statusCode = statusCode;
	}
	
	/**
	 * Get the status code.
	 * @return the status code
	 */
	public int getStatusCode() {
		return _statusCode;
	}
	
	/**
	 * Get the result type.
	 * @return result type
	 */
	public CommandResultType getType() {
		return _type;
	}
	
	/**
	 * The list of possible result types as recognized by the CLI API.
	 * @author Sean
	 *
	 */
	public enum CommandResultType {
		STANDARD,
		BAD_ARGS,
		ERROR,
		EXIT;
	}
}
