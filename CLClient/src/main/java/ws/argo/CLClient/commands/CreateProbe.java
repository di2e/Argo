package ws.argo.CLClient.commands;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
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
import ws.argo.wireline.probe.ProbeWrapper;

/**
 * This class will create a new probe that the CL UI can use over and over again when sending probes.
 *
 * <p>
 * The command structure is:
 * create
 *   -n : for future reference and use with 'send' command.
 *   -cid --clientID : sets the client ID in the probe.
 *   -scids --serviceContractIDs : takes a whitespace separated list strings to use as contract IDs
 *   -siids --serviceInstanceIDs : takes a whitespace separated list strings to use as instance IDs
 *   -ptype --respondToPayloadType : sets the respondTo payload type.  Must be JSON or XML or and error happens.
 *
 * @author jmsimpson
 *
 */
@CLICommand(name = "create", description = "Create a new probe")
public class CreateProbe extends Command<ArgoClientContext> {

  @Parameter(names = { "-n", "--name" }, description = "name of the probe for future reference.", required = true)
  private String _probeName;

  @Parameter(names = { "-cid", "--clientID" }, description = "cleint ID for the probe", required = false)
  private String _clientID;

  @Parameter(names = { "-scids", "--serviceContractIDs" }, variableArity = true)
  public List<String> _scids = new ArrayList<>();

  @Parameter(names = { "-siids", "--serviceInstanceIDs" }, variableArity = true)
  public List<String> _siids = new ArrayList<>();

  @Parameter(names = { "-ptype", "--respondToPayloadType" })
  private String _payloadType;

  private String getPayloadType() {
    if (_payloadType == null) {
      _payloadType = ProbeWrapper.XML;
    }

    return _payloadType;
  }

  private boolean validatePayloadType() {
    if (_payloadType == null) {
      _payloadType = ProbeWrapper.XML;
      return true;
    }

    if (!_payloadType.equals(Probe.JSON) || !_payloadType.equals(Probe.XML)) {
      return false;
    }
    return true;
  }

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {

    try {
      InetAddress localAddr = InetAddress.getLocalHost();
      // NetworkInterface ni = NetworkInterface.getByInetAddress(localAddr);
      String respondToURL = "http://" + localAddr.getHostAddress() + ":9998/listener/probeResponse";

      Probe probe = new Probe(getPayloadType());

      probe.addRespondToURL("cl-client", respondToURL);

      if (_clientID != null) {
        probe.setClientID(_clientID);
      }

      for (String scid : _scids) {
        probe.addServiceContractID(scid);
      }

      for (String siid : _siids) {
        probe.addServiceInstanceID(siid);
      }

      context.getProbes().put(_probeName, probe);

      Console.info("Created new probe named " + _probeName);

    } catch (UnsupportedPayloadType | UnknownHostException | MalformedURLException e) {
      e.printStackTrace();
      return CommandResult.BAD_ARGS;
    }

    return CommandResult.OK;
  }

}
