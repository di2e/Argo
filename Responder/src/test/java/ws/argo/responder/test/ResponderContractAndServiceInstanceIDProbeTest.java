package ws.argo.responder.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;

import org.junit.Test;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeGeneratorException;
import ws.argo.probe.UnsupportedPayloadType;
import ws.argo.wireline.probe.ProbeWrapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class ResponderContractAndServiceInstanceIDProbeTest extends ResponderProbeTest {

  @Test
  public void testContractID1JSON() throws UnsupportedPayloadType, InterruptedException, ProbeGeneratorException, MalformedURLException {
    final String targetContractId = "urn:uuid:dbbc5efa-42e3-418c-a753-d2f3392ada07";
    JsonArray serviceArray = probeForContractID(targetContractId, ProbeWrapper.JSON);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement contractIDElem = service.get("serviceContractId");
    String contractID = contractIDElem.getAsString();

    assertEquals(targetContractId, contractID);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testContractID1XML() throws UnsupportedPayloadType, IOException, InterruptedException, ProbeGeneratorException {
    final String targetContractId = "urn:uuid:dbbc5efa-42e3-418c-a753-d2f3392ada07";
    JsonArray serviceArray = probeForContractID(targetContractId, ProbeWrapper.XML);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement contractIDElem = service.get("serviceContractId");
    String contractID = contractIDElem.getAsString();

    assertEquals(targetContractId, contractID);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }


  @Test
  public void testContractID2JSON() throws UnsupportedPayloadType, InterruptedException, ProbeGeneratorException, MalformedURLException {
    final String targetContractId = "urn:uuid:4de22333-17ef-4028-b25c-6869ba080c08";
    JsonArray serviceArray = probeForContractID(targetContractId, ProbeWrapper.JSON);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);

    JsonElement contractIDElem = service.get("serviceContractId");

    String contractID = contractIDElem.getAsString();

    assertEquals(targetContractId, contractID);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testContractID2XML() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeGeneratorException {
    final String targetContractId = "urn:uuid:4de22333-17ef-4028-b25c-6869ba080c08";
    JsonArray serviceArray = probeForContractID(targetContractId, ProbeWrapper.XML);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);

    JsonElement contractIDElem = service.get("serviceContractId");

    String contractID = contractIDElem.getAsString();

    assertEquals(targetContractId, contractID);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

  @Test
  public void testUnknownContractID() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeGeneratorException {
    final String targetContractId = "nonexistent contract id";
    JsonArray serviceArray = probeForContractID(targetContractId,ProbeWrapper.JSON);

    assertEquals(0, serviceArray.size());

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testServiceInstanceID1JSON() throws UnsupportedPayloadType, InterruptedException, ProbeGeneratorException, MalformedURLException {
    final String targetServiceInstanceId = "urn:uuid:87362eb4-043b-4c75-b7e3-73462a7e9fce";
    JsonArray serviceArray = probeForServiceInstanceID(targetServiceInstanceId, ProbeWrapper.JSON);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement serviceInstanceIDElem = service.get("id");
    String serviceInstanceID = serviceInstanceIDElem.getAsString();

    assertEquals(targetServiceInstanceId, serviceInstanceID);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testServiceInstanceID1XML() throws UnsupportedPayloadType, InterruptedException, ProbeGeneratorException, MalformedURLException {
    final String targetServiceInstanceId = "urn:uuid:87362eb4-043b-4c75-b7e3-73462a7e9fce";
    JsonArray serviceArray = probeForServiceInstanceID(targetServiceInstanceId, ProbeWrapper.XML);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement serviceInstanceIDElem = service.get("id");
    String serviceInstanceID = serviceInstanceIDElem.getAsString();

    assertEquals(targetServiceInstanceId, serviceInstanceID);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testServiceInstanceID2JSON() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeGeneratorException {
    final String targetServiceInstanceId = "urn:uuid:bd61b54b-93d1-4c9a-81b7-c189c383f459";
    JsonArray serviceArray = probeForServiceInstanceID(targetServiceInstanceId, ProbeWrapper.JSON);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement serviceInstanceIDElem = service.get("id");
    String serviceInstanceID = serviceInstanceIDElem.getAsString();

    assertEquals(targetServiceInstanceId, serviceInstanceID);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testServiceInstanceID2XML() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeGeneratorException {
    final String targetServiceInstanceId = "urn:uuid:bd61b54b-93d1-4c9a-81b7-c189c383f459";
    JsonArray serviceArray = probeForServiceInstanceID(targetServiceInstanceId, ProbeWrapper.XML);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement serviceInstanceIDElem = service.get("id");
    String serviceInstanceID = serviceInstanceIDElem.getAsString();

    assertEquals(targetServiceInstanceId, serviceInstanceID);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

  private JsonArray probeForContractID(String targetContractId, String payloadType) throws UnsupportedPayloadType, InterruptedException, ProbeGeneratorException, MalformedURLException {
    Probe probe = new Probe(payloadType);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");

    // this urn is from the services in the test resources
    probe.addServiceContractID(targetContractId);

    return sendProbe(probe);

  }
  
  private JsonArray probeForServiceInstanceID(String targetServiceInstanceId, String payloadType) throws UnsupportedPayloadType, InterruptedException, ProbeGeneratorException, MalformedURLException {
    Probe probe = new Probe(payloadType);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");

    // this urn is from the services in the test resources
    probe.addServiceInstanceID(targetServiceInstanceId);

    return sendProbe(probe);

  }

  private JsonArray sendProbe(Probe probe) throws InterruptedException, ProbeGeneratorException {
    gen.sendProbe(probe);
    Thread.sleep(500); // let the responder process the message and post back
    // to the listener

    String responseMsg = target.path("listener/responses").get(String.class);

    JsonReader reader = new JsonReader(new StringReader(responseMsg));

    JsonParser jsonParser = new JsonParser();
    JsonObject cacheObject = jsonParser.parse(reader).getAsJsonObject();

    JsonArray serviceArray = cacheObject.getAsJsonArray("cache");

    return serviceArray;
  }

}
