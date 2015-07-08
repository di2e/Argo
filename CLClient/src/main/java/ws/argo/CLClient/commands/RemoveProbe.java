package ws.argo.CLClient.commands;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import ws.argo.CLClient.ArgoClientContext;

@CLICommand(name = "remove", description = "remove a probe previously created or imported.")
public class RemoveProbe extends Command<ArgoClientContext> {

  @Parameter(names = { "-n", "--probeNames" }, variableArity = true, description = "list of probe names to launch")
  public List<String> _probeNames = new ArrayList<>();

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {

    return CommandResult.OK;
  }

}
