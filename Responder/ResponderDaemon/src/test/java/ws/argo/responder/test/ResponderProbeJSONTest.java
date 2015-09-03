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
import ws.argo.probe.ProbeGeneratorException;
import ws.argo.probe.UnsupportedPayloadType;

public class ResponderProbeJSONTest extends ResponderProbeTest {

  private String nakedProbeJSONResponseFromListener;

  /**
   * reads in the test payload text to check responses against. better then
   * putting it in the source code.
   * 
   * @throws IOException if the resource is missing
   */
  @Before
  public void readTargetXMLFiles() throws IOException {
    // Read the completely filled out probe test file for comparison
    assertNotNull("nakedProbeJSONResponseFromListener.json file missing", ResponderProbeJSONTest.class.getResource("/nakedProbeJSONResponseFromListener.json"));
    try (InputStream is = ResponderProbeJSONTest.class.getResourceAsStream("/nakedProbeJSONResponseFromListener.json")) {
      nakedProbeJSONResponseFromListener = IOUtils.toString(is, "UTF-8");
    }

  }

  @Test
  public void testNakedProbeJSON() throws UnsupportedPayloadType, InterruptedException, MalformedURLException, ProbeGeneratorException {
    Probe probe = new Probe(Probe.JSON);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");
    gen.sendProbe(probe); // Send the naked probe
    Thread.sleep(1000); // let the responder process the message and post back
    // to the listener

    System.out.println("Getting testNakedProbeJSON cached responses from listener");

    String responseMsg = target.path("listener/responses").request().get(String.class);
    assertEquals(nakedProbeJSONResponseFromListener, responseMsg);

    String cacheClearedMsg = target.path("listener/clearCache").request().get(String.class);
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

}
