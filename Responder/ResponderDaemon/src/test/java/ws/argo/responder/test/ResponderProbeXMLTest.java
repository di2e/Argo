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

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSenderException;
import ws.argo.probe.UnsupportedPayloadType;
import ws.argo.wireline.probe.ProbeWrapper;

/**
 * Probes with XML as the payload.
 * 
 * @author jmsimpson
 *
 */
public class ResponderProbeXMLTest extends ResponderProbeTest {

  private  String      nakedProbeXMLResponseFromListener;

  /**
   * reads in the test payload text to check responses against. better then
   * putting it in the source code.
   * 
   * @throws IOException if the resource is missing
   */
  @Before
  public void readTargetXMLFiles() throws IOException {

    // Read the completely filled out probe test file for comparison
    assertNotNull("nakedProbeXMLResponseFromListener.json file missing", ResponderProbeTest.class.getResource("/nakedProbeXMLResponseFromListener.json"));
    try (InputStream is = ResponderProbeTest.class.getResourceAsStream("/nakedProbeXMLResponseFromListener.json")) {
      nakedProbeXMLResponseFromListener = IOUtils.toString(is, "UTF-8");
    }

  }
  
  @Test
  public void testNakedProbeXML() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeSenderException {
    Probe probe = new Probe(ProbeWrapper.XML);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");
    gen.sendProbe(probe); // Send the naked probe
    Thread.sleep(2000); // let the responder process the message and post back
                        // to the listener
    
    System.out.println("Getting testNakedProbeXML cached responses from listener");

    String responseMsg = target.path("listener/responses").request().get(String.class);
    assertEquals(nakedProbeXMLResponseFromListener.length(), responseMsg.length());

    String cacheClearedMsg = target.path("listener/clearCache").request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

}
