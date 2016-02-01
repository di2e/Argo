/*
 * Copyright 2015 Jeff Simpson.
 *
 * Licensed under the MIT License, (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.argo.responder.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;

import javax.ws.rs.client.WebTarget;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSenderException;
import ws.argo.probe.UnsupportedPayloadType;

/**
 * Checks for specific queries.
 * 
 * @author jmsimpson
 *
 */
public class ResponderContractAndServiceInstanceIDProbeTest extends ResponderProbeTest {

  @Test
  public void testContractID1JSON() throws UnsupportedPayloadType, InterruptedException, ProbeSenderException, MalformedURLException {
    final String targetContractId = "urn:uuid:dbbc5efa-42e3-418c-a753-d2f3392ada07";
    JsonArray serviceArray = probeForContractID(targetContractId, Probe.JSON);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement contractIDElem = service.get("serviceContractId");
    String contractID = contractIDElem.getAsString();

    assertEquals(targetContractId, contractID);

    WebTarget cacheTarget = target.path("listener/clearCache");
    String cacheClearedMsg = cacheTarget.request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testContractID1XML() throws UnsupportedPayloadType, IOException, InterruptedException, ProbeSenderException {
    final String targetContractId = "urn:uuid:dbbc5efa-42e3-418c-a753-d2f3392ada07";
    JsonArray serviceArray = probeForContractID(targetContractId, Probe.XML);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement contractIDElem = service.get("serviceContractId");
    String contractID = contractIDElem.getAsString();

    assertEquals(targetContractId, contractID);

    WebTarget cacheTarget = target.path("listener/clearCache");
    String cacheClearedMsg = cacheTarget.request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }


  @Test
  public void testContractID2JSON() throws UnsupportedPayloadType, InterruptedException, ProbeSenderException, MalformedURLException {
    final String targetContractId = "urn:uuid:4de22333-17ef-4028-b25c-6869ba080c08";
    JsonArray serviceArray = probeForContractID(targetContractId, Probe.JSON);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);

    JsonElement contractIDElem = service.get("serviceContractId");

    String contractID = contractIDElem.getAsString();

    assertEquals(targetContractId, contractID);

    String cacheClearedMsg = target.path("listener/clearCache").request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testContractID2XML() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeSenderException {
    final String targetContractId = "urn:uuid:4de22333-17ef-4028-b25c-6869ba080c08";
    JsonArray serviceArray = probeForContractID(targetContractId, Probe.XML);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);

    JsonElement contractIDElem = service.get("serviceContractId");

    String contractID = contractIDElem.getAsString();

    assertEquals(targetContractId, contractID);

    String cacheClearedMsg = target.path("listener/clearCache").request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

  @Test
  public void testUnknownContractID() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeSenderException {
    final String targetContractId = "nonexistent contract id";
    JsonArray serviceArray = probeForContractID(targetContractId,Probe.JSON);

    assertEquals(0, serviceArray.size());

    String cacheClearedMsg = target.path("listener/clearCache").request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testServiceInstanceID1JSON() throws UnsupportedPayloadType, InterruptedException, ProbeSenderException, MalformedURLException {
    final String targetServiceInstanceId = "urn:uuid:87362eb4-043b-4c75-b7e3-73462a7e9fce";
    JsonArray serviceArray = probeForServiceInstanceID(targetServiceInstanceId, Probe.JSON);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement serviceInstanceIDElem = service.get("id");
    String serviceInstanceID = serviceInstanceIDElem.getAsString();

    assertEquals(targetServiceInstanceId, serviceInstanceID);

    String cacheClearedMsg = target.path("listener/clearCache").request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testServiceInstanceID1XML() throws UnsupportedPayloadType, InterruptedException, ProbeSenderException, MalformedURLException {
    final String targetServiceInstanceId = "urn:uuid:87362eb4-043b-4c75-b7e3-73462a7e9fce";
    JsonArray serviceArray = probeForServiceInstanceID(targetServiceInstanceId, Probe.XML);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement serviceInstanceIDElem = service.get("id");
    String serviceInstanceID = serviceInstanceIDElem.getAsString();

    assertEquals(targetServiceInstanceId, serviceInstanceID);

    String cacheClearedMsg = target.path("listener/clearCache").request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testServiceInstanceID2JSON() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeSenderException {
    final String targetServiceInstanceId = "urn:uuid:bd61b54b-93d1-4c9a-81b7-c189c383f459";
    JsonArray serviceArray = probeForServiceInstanceID(targetServiceInstanceId, Probe.JSON);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement serviceInstanceIDElem = service.get("id");
    String serviceInstanceID = serviceInstanceIDElem.getAsString();

    assertEquals(targetServiceInstanceId, serviceInstanceID);

    String cacheClearedMsg = target.path("listener/clearCache").request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }
  
  @Test
  public void testServiceInstanceID2XML() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeSenderException {
    final String targetServiceInstanceId = "urn:uuid:bd61b54b-93d1-4c9a-81b7-c189c383f459";
    JsonArray serviceArray = probeForServiceInstanceID(targetServiceInstanceId, Probe.XML);

    assertEquals(1, serviceArray.size());

    JsonObject service = (JsonObject) serviceArray.get(0);
    JsonElement serviceInstanceIDElem = service.get("id");
    String serviceInstanceID = serviceInstanceIDElem.getAsString();

    assertEquals(targetServiceInstanceId, serviceInstanceID);

    String cacheClearedMsg = target.path("listener/clearCache").request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

  private JsonArray probeForContractID(String targetContractId, String payloadType) throws UnsupportedPayloadType, InterruptedException, ProbeSenderException, MalformedURLException {
    Probe probe = new Probe(payloadType);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");

    // this urn is from the services in the test resources
    probe.addServiceContractID(targetContractId);

    return sendProbe(probe);

  }
  
  private JsonArray probeForServiceInstanceID(String targetServiceInstanceId, String payloadType) throws UnsupportedPayloadType, InterruptedException, ProbeSenderException, MalformedURLException {
    Probe probe = new Probe(payloadType);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");

    // this urn is from the services in the test resources
    probe.addServiceInstanceID(targetServiceInstanceId);

    return sendProbe(probe);

  }

  private JsonArray sendProbe(Probe probe) throws InterruptedException, ProbeSenderException {
    gen.sendProbe(probe);
    Thread.sleep(500); // let the responder process the message and post back
    // to the listener

    String responseMsg = target.path("listener/responses").request().get(String.class);

    JsonReader reader = new JsonReader(new StringReader(responseMsg));

    JsonParser jsonParser = new JsonParser();
    JsonObject cacheObject = jsonParser.parse(reader).getAsJsonObject();

    JsonArray serviceArray = cacheObject.getAsJsonArray("cache");

    return serviceArray;
  }

}
