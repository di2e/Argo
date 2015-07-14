package ws.argo.CLClient.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.beust.jcommander.Parameter;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeGenerator;
import ws.argo.probe.ProbeGeneratorException;

@CLICommand(name = "send", description = "send the probe")
public class SendProbe extends Command<ArgoClientContext> {

  @Parameter(description = "list of probe names")
  public List<String> _probeNames = new ArrayList<>();

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {

    if (_probeNames.isEmpty()) {
      return sendAllProbes(context);
    } else {
      return sendProbes(context, _probeNames);
    }

  }

  private CommandResult sendProbes(ArgoClientContext context, List<String> probeNames) {
    Console.error("Sending specified list of probes.");
    for (String probeName : probeNames) {
      Probe probe = context.getProbe(probeName);
      if (probe != null) {
        sendProbe(context, probeName, probe);
      } else {
        Console.error("Unknown probe name specified: " + probeName);
      }
    }
    return CommandResult.OK;
  }

  private CommandResult sendAllProbes(ArgoClientContext context) {
    Console.error("Empty list of probe names - sending all probes");
    HashMap<String, Probe> probes = context.getProbes();
    for (Entry<String, Probe> entry : probes.entrySet()) {
      sendProbe(context, entry.getKey(), entry.getValue());
    }
    return CommandResult.OK;
  }

  private CommandResult sendProbe(ArgoClientContext context, String probeName, Probe probe) {

    Map<String, ProbeGenerator> probeGens = context.getProbeGenerators();

    probe.recreateProbeID();

    for (ProbeGenerator probeGen : probeGens.values()) {
      try {
        if (context.getNIList().contains(probeGen.getNIName())) {
          probeGen.sendProbe(probe);
          Console.info("Sent probe " + probeName + " on network interface [" + probeGen.getNIName() + "]");
        }
      } catch (ProbeGeneratorException e) {
        Console.error("Probe failed: " + probeName);
        Console.error(e.getMessage());
        return CommandResult.ERROR;
      }
    }

    return CommandResult.OK;
  }

}
