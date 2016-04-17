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
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSenderException;
import ws.argo.probe.UnsupportedPayloadType;

/**
 * Test the Responder using JSON as the payload format.
 * 
 * @author jmsimpson
 *
 */
public class ResponderProbeJSONTest extends ResponderProbeTest {

  private String nakedProbeJSONResponseFromListener;

  /**
   * reads in the test payload text to check responses against. better then
   * putting it in the source code.
   * 
   * @throws IOException if the resource is missing
   */
  //@Before
  public void readTargetXMLFiles() throws IOException {
    // Read the completely filled out probe test file for comparison
    assertNotNull("nakedProbeJSONResponseFromListener.json file missing", ResponderProbeJSONTest.class.getResource("/nakedProbeJSONResponseFromListener.json"));
    try (InputStream is = ResponderProbeJSONTest.class.getResourceAsStream("/nakedProbeJSONResponseFromListener.json")) {
      nakedProbeJSONResponseFromListener = IOUtils.toString(is, "UTF-8");
    }

  }

  //@Test
  public void testNakedProbeJSON() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeSenderException {
    Probe probe = new Probe(Probe.JSON);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");
    gen.sendProbe(probe); // Send the naked probe
    Thread.sleep(2000); // let the responder process the message and post back
    // to the listener

    System.out.println("Getting testNakedProbeJSON cached responses from listener");

    String responseMsg = target.path("listener/responses").request().get(String.class);
//    Gson gson = new Gson();
    
//    JSONObject responseJSON = gson.fromJson(responseMsg, JSONObject.class);
    
    assertEquals(nakedProbeJSONResponseFromListener, responseMsg);

    String cacheClearedMsg = target.path("listener/clearCache").request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

}
