package ws.argo.CLClient.commands;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;
import ws.argo.probe.Probe;
import ws.argo.probe.UnsupportedPayloadType;

/**
 * 
 * @author jmsimpson
 *
 */
@CLICommand(name = "modify", description = "modify an existing probe.")
public class ModifyProbe extends Command<ArgoClientContext> {

  @Parameter(names = { "-n", "--name" }, description = "name of the probe.", required = true)
  private String _probeName;

  @Parameter(names = { "-cid", "--clientID" }, description = "cleint ID for the probe")
  private String _clientID;

  @Parameter(names = { "-scids", "--serviceContractIDs" }, variableArity = true)
  public List<String> _scids = new ArrayList<>();

  @Parameter(names = { "-siids", "--serviceInstanceIDs" }, variableArity = true)
  public List<String> _siids = new ArrayList<>();

  @Parameter(names = { "-ptype", "--respondToPayloadType" })
  private String _payloadType;

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {

    Probe probe = context.getProbe(_probeName);
    if (probe == null) {
      Console.error("No exising probe named [" + _probeName + "]");
      return CommandResult.ERROR;
    }

    if (_payloadType != null) {
      try {
        probe.setRespondToPayloadType(_payloadType);
      } catch (UnsupportedPayloadType e) {
        Console.error("Unsupported payload type [" + _payloadType + "]");
        return CommandResult.BAD_ARGS;
      }
    }

    if (_clientID != null) {
      probe.setClientID(_clientID);
    }

    if (!_scids.isEmpty()) {
      for (String serviceContractID : _scids) {
        probe.addServiceContractID(serviceContractID);
      }
    }

    if (!_siids.isEmpty()) {
      for (String serviceInstanceID : _scids) {
        probe.addServiceInstanceID(serviceInstanceID);
      }
    }

    return CommandResult.OK;
  }

}
