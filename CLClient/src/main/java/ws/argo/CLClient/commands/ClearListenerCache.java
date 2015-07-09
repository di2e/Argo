package ws.argo.CLClient.commands;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;

/**
 * This command will send a REST call the the embedded JAX-RS server to clear
 * the cache of the response listener.
 * 
 * @author jmsimpson
 *
 */
@CLICommand(name = "clear", description = "clear the response listener cache.")
public class ClearListenerCache extends Command<ArgoClientContext> {

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {
    String responseMsg = context.getListenerTarget().path("listener/clearCache").get(String.class);
    Console.info(responseMsg);
    return CommandResult.OK;
  }

}
