package ws.argo.CLClient.commands.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ws.argo.common.cache.ResponseCache;
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

  private List<ServiceWrapper> _cache       = new ArrayList<ServiceWrapper>();
  private Set<String>          _probeIds    = new HashSet<String>();
  private Set<String>          _responseIds = new HashSet<String>();

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
      _cache.add(r);
      _probeIds.add(r.getProbeId());
      _responseIds.add(r.getResponseId());
    }
  }

  public int numProbes() {
    return _probeIds.size();
  }

  public int numResponses() {
    return _responseIds.size();
  }

  /**
   * Generate a list of the descriptions of the cache entries (services).
   * 
   * @param _ids list of the ids to use
   * @param payload if you want to see the payloads
   * @param pretty if you want those payloads pretty printed
   * @return list of description strings
   */
  public List<String> descriptionsForIds(List<String> _ids, boolean payload, boolean pretty) {
    List<String> descriptions = new ArrayList<String>();
  
    for (ServiceWrapper s : _cache) {
      if (_ids.isEmpty() || _ids.contains(s.id)) {
        StringBuffer buf = serviceDesciption(payload, pretty, s);
        descriptions.add(buf.toString());
      }
    }
  
    return descriptions;
  }

  /**
   * Return the list of description strings for a list of response ids.
   * 
   * @param _ids list of response ids
   * @param payload print the whole payload
   * @param pretty make it pretty
   * @return the string of all the specified response payloads
   */
  public List<String> descriptionsForResponseIDs(List<String> _ids, boolean payload, boolean pretty) {
    List<String> descriptions = new ArrayList<String>();

    for (ServiceWrapper s : _cache) {
      if (_ids.isEmpty() || _responseIds.contains(s.getResponseId())) {
        StringBuffer buf = serviceDesciption(payload, pretty, s);
        descriptions.add(buf.toString());
      }
    }

    return descriptions;
  }

  /**
   * Return the list of description strings for a list of probe ids.
   * 
   * @param _ids list of response ids
   * @param payload print the whole payload
   * @param pretty pretty make it pretty
   * @return the string of all the specified response payloads
   */
  public List<String> descriptionsForProbeIDs(List<String> _ids, boolean payload, boolean pretty) {
    List<String> descriptions = new ArrayList<String>();

    for (ServiceWrapper s : _cache) {
      if (_ids.isEmpty() || _probeIds.contains(s.getProbeId())) {
        StringBuffer buf = serviceDesciption(payload, pretty, s);
        descriptions.add(buf.toString());
      }
    }

    return descriptions;
  }

  private StringBuffer serviceDesciption(boolean payload, boolean pretty, ServiceWrapper s) {
    JSONSerializer ser = new JSONSerializer();
    Gson gson = new Gson();
    Gson prettyJson = new GsonBuilder().setPrettyPrinting().create();

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
    return buf;
  }

  /**
   * Return the cache as JSON.
   * 
   * @param pretty make it pretty
   * @return the string as JSON
   */
  public String asJSON(boolean pretty) {
    JSONSerializer ser = new JSONSerializer();
    Gson gson = new Gson();
    Gson prettyJson = new GsonBuilder().setPrettyPrinting().create();

    JsonArray cacheArray = new JsonArray();

    String cacheAsString;

    for (ServiceWrapper s : _cache) {
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
