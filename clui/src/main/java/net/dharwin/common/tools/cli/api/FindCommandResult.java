package net.dharwin.common.tools.cli.api;

public class FindCommandResult {

  private Command<? extends CLIContext> foundCommand;
  private String[] remainingArguments;
  
  public FindCommandResult(Command<? extends CLIContext> command, String[] args) {
    this.foundCommand = command;
    this.remainingArguments = args;
  }

  public Command<? extends CLIContext> getFoundCommand() {
    return foundCommand;
  }

  public String[] getRemainingArguments() {
    return remainingArguments;
  }
  
}
