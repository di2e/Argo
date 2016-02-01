package ws.argo.CLClient.commands;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.WebTarget;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.CompoundCommand;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;
import ws.argo.CLClient.commands.util.Cache;
import ws.argo.wireline.response.ResponseParseException;

/**
 * This class encapsulated the listener cache related commands.
 * 
 * @author jmsimpson
 *
 */
@CLICommand(name = "cache", description = "manage the cache of probe responses")
public class CacheCommand extends CompoundCommand<ArgoClientContext> {

  private Cache createCacheFromListener(ArgoClientContext context) {
    Cache cache = null;

    Console.superFine("Getting the responses from the listener ...");
    String responseMsg = context.getConfig().getListenerTarget().path("listener/responses").request().get(String.class);
    try {
      cache = new Cache(responseMsg);
    } catch (ResponseParseException e) {
      Console.error("Error parsing the response message [" + responseMsg + "] - " + e.getLocalizedMessage());
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return cache;
  }

  /**
   * This command will send a REST call the the embedded JAX-RS server to clear
   * the cache of the response listener.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "export" }, commandDescription = "exports the cache to a file.")
  public class ExportCache extends Command<ArgoClientContext> {

    @Parameter(names = { "-pretty", "--prettyPayloads" }, description = "pretty print the JSON payloads.")
    private boolean _prettyPayload;

    @Parameter(names = { "-fn", "--filename" }, description = "filename to export to.")
    private String _filename;

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      Cache cache = createCacheFromListener(context);

      if (cache != null) {
        
        String cacheAsString = cache.asJSON(_prettyPayload);
        try (FileWriter writer = new FileWriter(_filename)) {
          writer.write(cacheAsString);
        } catch (IOException e) {
          Console.error("There was an error trying to write the file [" + _filename + "] - " + e.getLocalizedMessage());
          return CommandResult.ERROR;
        }

      } else {
        Console.info("There are no cache results to export.  Aborting command.");
      }
      
      return CommandResult.OK;

    }
  }

  /**
   * This command will send a REST call the the embedded JAX-RS server to clear
   * the cache of the response listener.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "clear" }, commandDescription = "clear the response listener cache.")
  public class ClearListenerCache extends Command<ArgoClientContext> {

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {
      WebTarget target = context.getConfig().getListenerTarget();
      String responseMsg = target.path("listener/clearCache").request().get(String.class);
      return CommandResult.OK;
    }

  }

  /**
   * This command will send a REST call the the embedded JAX-RS server to clear
   * the cache of the response listener.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "list" }, commandDescription = "list the basic response cache entries.")
  public class ListListenerCache extends Command<ArgoClientContext> {

    @Parameter(description = "<service id list>")
    public List<String> _ids = new ArrayList<String>();

    @Parameter(names = { "-p", "--payloads" }, description = "show the full payloads")
    private boolean _showPayload;

    @Parameter(names = { "-pretty", "--prettyPayloads" }, description = "pretty print the JSON payloads.")
    private boolean _prettyPayload;

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      Cache cache = createCacheFromListener(context);

      if (cache != null) {
        
        int i = 1;
        for (String desc : cache.descriptionsForIds(_ids, _showPayload, _prettyPayload)) {
          Console.info(i + ": " + desc);
          i++;
        }
      } else {
        Console.info("Empty cache.");
      }

      return CommandResult.OK;
    }

  }
  
  /**
   * This command will send a REST call the the embedded JAX-RS server to clear
   * the cache of the response listener.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "match" }, commandDescription = "list entries that match probe names.")
  public class MatchListenerCache extends Command<ArgoClientContext> {

    @Parameter(description = "<probe names to match>")
    public List<String> _probeNames = new ArrayList<String>();

    @Parameter(names = { "-p", "--payloads" }, description = "show the full payloads")
    private boolean _showPayload;

    @Parameter(names = { "-pretty", "--prettyPayloads" }, description = "pretty print the JSON payloads.")
    private boolean _prettyPayload;

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      Cache cache = createCacheFromListener(context);

      if (cache != null) {

        List<String> pids = new ArrayList<String>();
        for (String name : _probeNames) {
          if (context.getSentProbes().containsKey(name)) {
            pids.add(context.getProbe(name).getProbeID());
          } else {
            Console.warn("Probe named [" + name + "] is not in the sent probes list. Ignoring.");
          }
            
        }
        
        int i = 1;
        for (String desc : cache.descriptionsForProbeIDs(pids, _showPayload, _prettyPayload)) {
          Console.info(i + ": " + desc);
          i++;
        }
      } else {
        Console.info("Empty cache.");
      }

      return CommandResult.OK;
    }

  }

}
