package ws.argo.CLClient.commands;

import com.beust.jcommander.Parameter;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;
import ws.argo.probe.Probe;

@CLICommand(name = "delete", description = "deletes an existing probe")
public class DeleteProbe extends Command<ArgoClientContext> {

  @Parameter(names = { "-n", "--name" }, description = "name of the probe.", required = true)
  private String _probeName;

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {
    Probe probe = context.getProbe(_probeName);
    if (probe == null) {
      Console.error("No exising probe named [" + _probeName + "]");
      return CommandResult.ERROR;
    } else {
      context.getProbes().remove(_probeName);
    }

    Console.info("Deleted probe named [" + _probeName + "]");
    return CommandResult.OK;
  }

}
