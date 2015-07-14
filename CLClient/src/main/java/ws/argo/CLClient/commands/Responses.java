package ws.argo.CLClient.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;

@CLICommand(name = "responses", description = "list the cached probe responses.")
public class Responses extends Command<ArgoClientContext> {

  @Override
  protected CommandResult innerExecute(ArgoClientContext context) {
    Console.info("Getting the responses ...");
    String responseMsg = context.getListenerTarget().path("listener/responses").get(String.class);

    Gson gson = new Gson();

    JsonObject cache = gson.fromJson(responseMsg, JsonObject.class);

    JsonArray services = cache.get("cache").getAsJsonArray();

    Gson prettyJson = new GsonBuilder().setPrettyPrinting().create();

    Console.info("Listing " + services.size() + " responses from the listener cache.");
    for (JsonElement service : services) {
      String jsonString = prettyJson.toJson(service);
      Console.info(jsonString);
    }

    return CommandResult.OK;
  }

}
