package ws.argo.CLClient.commands.util;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ws.argo.CLClient.listener.ResponseCache;
import ws.argo.wireline.response.JSONSerializer;
import ws.argo.wireline.response.ResponseParseException;
import ws.argo.wireline.response.ServiceWrapper;

/**
 * This class is a representation of the response cache for the purposes of this
 * command. It is not the actual client listener cache with is the
 * {@linkplain ResponseCache}.
 * 
 * @author jmsimpson
 *
 */
public class Cache {

  private List<ServiceWrapper> cache = new ArrayList<ServiceWrapper>();

  public Cache(String json) throws ResponseParseException {
    parseJson(json);
  }

  private void parseJson(String json) throws ResponseParseException {

    Gson gson = new Gson();
    JsonObject cache = gson.fromJson(json, JsonObject.class);
    JsonArray services = cache.get("cache").getAsJsonArray();

    JSONSerializer serializer = new JSONSerializer();

    for (JsonElement service : services) {
      String jsonString = service.toString();
      ServiceWrapper r = serializer.unmarshalService(jsonString);
      this.cache.add(r);
    }
  }

  /**
   * Generate a list of the descriptions of the cache entries (services).
   * 
   * @return list of description strings
   */
  public List<String> descriptions(List<String> _ids, boolean payload, boolean pretty) {
    List<String> descriptions = new ArrayList<String>();
    JSONSerializer ser = new JSONSerializer();
    Gson gson = new Gson();
    Gson prettyJson = new GsonBuilder().setPrettyPrinting().create();

    for (ServiceWrapper s : cache) {
      if (_ids.isEmpty() || _ids.contains(s.id)) {
        StringBuffer buf = new StringBuffer();
        buf.append(s.getServiceName()).append(" [").append(s.getDescription()).append("] : ").append(s.getId());
        if (payload) {
          String json = ser.marshalService(s);
          if (pretty) {
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            String prettyJSON = prettyJson.toJson(obj);
            buf.append("\n").append(prettyJSON).append("\n");
          } else {
            buf.append("\n").append(json).append("\n");
          }
        }
        descriptions.add(buf.toString());
      }
    }

    return descriptions;
  }

  /**
   * 
   * @param pretty
   * @return
   */
  public String asJSON(boolean pretty) {
    JSONSerializer ser = new JSONSerializer();
    Gson gson = new Gson();
    Gson prettyJson = new GsonBuilder().setPrettyPrinting().create();

    JsonArray cacheArray = new JsonArray();
    
    String cacheAsString;

    for (ServiceWrapper s : cache) {
      String json = ser.marshalService(s);
      JsonObject jsonService = gson.fromJson(json, JsonObject.class);
      cacheArray.add(jsonService);
    }

    if (pretty) {
      cacheAsString = prettyJson.toJson(cacheArray);
    } else {
      cacheAsString = gson.toJson(cacheArray);
    }

    return cacheAsString;
  }

}
