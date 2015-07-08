package net.dharwin.common.tools.cli.api.utils;

import net.dharwin.common.tools.cli.api.CLIContext;
import net.dharwin.common.tools.cli.api.Command;

/**
 * CommandUtils contains functions helpful for command operations, such as
 * looking up commands by name.
 * TODO: Integrate this class into other code locations that are manually looking
 * up command classes.
 */
public class CommandUtils {
    
    /**
     * Get a command by name.
     * @param context The CLIContext
     * @param commandName The name of the command.
     * @return The command, or null if not found.
     */
    public static Class<? extends Command<? extends CLIContext>> getCommandClass(
            CLIContext context, String commandName) {
        return context.getHostApplication().getCommands().get(commandName.toLowerCase());
    }
}
