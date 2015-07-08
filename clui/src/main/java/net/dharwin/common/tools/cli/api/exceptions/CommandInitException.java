package net.dharwin.common.tools.cli.api.exceptions;

public class CommandInitException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6370335834989940601L;
	
	private String _commandName;
	
	public CommandInitException(String commandName) {
		_commandName = commandName;
	}
	
	public String getCommandName() {
		return _commandName;
	}
}
