package ws.argo.CLClient.commands;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;

@CLICommand(name = "clear", description = "clear the response listener cache.")
public class ClearListenerCache extends Command<ArgoClientContext> {

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {
    String responseMsg = context.getListenerTarget().path("listener/clearCache").get(String.class);
    Console.info(responseMsg);
    return CommandResult.OK;
  }

}
