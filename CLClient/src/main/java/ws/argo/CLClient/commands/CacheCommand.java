package ws.argo.CLClient.commands;

import java.util.ArrayList;
import java.util.List;

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
    String responseMsg = context.getListenerTarget().path("listener/responses").get(String.class);
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
  @Parameters(commandNames = { "clear" }, commandDescription = "clear the response listener cache.")
  public class ClearListenerCache extends Command<ArgoClientContext> {

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {
      String responseMsg = context.getListenerTarget().path("listener/clearCache").get(String.class);
      Console.info(responseMsg);
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

    @Parameter
    public List<String> _ids = new ArrayList<>();
    
    @Parameter(names = { "-p", "--payloads"}, description = "show the full payloads")
    private boolean _showPayload;

    @Parameter(names = { "-pretty", "--prettyPayloads"}, description = "pretty print the JSON payloads.")
    private boolean _prettyPayload;

    
    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      Cache cache = createCacheFromListener(context);

      if (cache != null) {

        int i = 1;
        for (String desc : cache.descriptions(_ids, _showPayload, _prettyPayload)) {
          Console.info(i + ": " + desc);
          i++;
        }
      }

      return CommandResult.OK;
    }

  }

}
