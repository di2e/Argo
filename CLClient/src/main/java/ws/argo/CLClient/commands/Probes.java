package ws.argo.CLClient.commands;

import java.util.HashMap;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;
import ws.argo.probe.Probe;

@CLICommand(name = "probes", description = "list or stored probes")
public class Probes extends Command<ArgoClientContext> {

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {
    HashMap<String, Probe> probes = context.getProbes();

    Console.info("Current probes:");
    for (Entry<String, Probe> entry : probes.entrySet()) {
      String probeName = entry.getKey();
      Probe probe = entry.getValue();
      Console.info("Probe name=" + probeName);
      try {
        String probeInfo = probe.asXML();
        Console.info(probeInfo);
      } catch (JAXBException e) {
        Console.error("Error serializing probe: " + e.getLocalizedMessage());
      }
    }

    return CommandResult.OK;
  }

}
