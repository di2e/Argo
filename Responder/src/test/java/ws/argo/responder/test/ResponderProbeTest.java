package ws.argo.responder.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeGenerator;
import ws.argo.probe.UnsupportedPayloadType;
import ws.argo.responder.test.listener.ResponseListener;
import ws.argo.responder.Responder;
import ws.argo.responder.ResponderConfigException;
import ws.argo.responder.ResponderOperationException;
import ws.argo.wireline.probe.ProbeWrapper;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class ResponderProbeTest {

  private static HttpServer  server;
  private static WebResource target;
  private static Thread      responderThread;
  static ProbeGenerator      gen = null;

  private static String      nakedProbeJSONResponseFromListener;
  private static String nakedProbeXMLResponseFromListener;

  /**
   * reads in the test payload text to check responses against. better then
   * putting it in the source code.
   * 
   * @throws IOException if the resource is missing
   */
  private static void readTargetXMLFiles() throws IOException {
    // Read the completely filled out probe test file for comparison
    assertNotNull("nakedProbeJSONResponseFromListener.json file missing", ResponderProbeTest.class.getResource("/nakedProbeJSONResponseFromListener.json"));
    try (InputStream is = ResponderProbeTest.class.getResourceAsStream("/nakedProbeJSONResponseFromListener.json")) {
      nakedProbeJSONResponseFromListener = IOUtils.toString(is, "UTF-8");
    }

    // Read the completely filled out probe test file for comparison
    assertNotNull("nakedProbeXMLResponseFromListener.json file missing", ResponderProbeTest.class.getResource("/nakedProbeXMLResponseFromListener.json"));
    try (InputStream is = ResponderProbeTest.class.getResourceAsStream("/nakedProbeXMLResponseFromListener.json")) {
      nakedProbeXMLResponseFromListener = IOUtils.toString(is, "UTF-8");
    }

  }

  private static void startListener() throws IOException {
    server = ResponseListener.startServer();
  
    target = Client.create().resource(ResponseListener.BASE_URI);
  }

  private static void startResponder() {

    String configFileProp = System.getProperty("configFile");
    System.out.println("****** Testing configFile = " + configFileProp);
    final String[] args = { "-pf", configFileProp };

    responderThread = new Thread() {
      public void run() {
        try {
          Responder.main(args);
        } catch (ResponderConfigException e) {
          org.junit.Assert.fail(e.getLocalizedMessage());
        } catch (ResponderOperationException e) {
          org.junit.Assert.fail(e.getLocalizedMessage());
        }
      }
    };
    responderThread.start();
  }

  /**
   * Start up the necessary gear to test Argo. Multiple process are involved
   * here that use multicast networking UDP packets. Make sure that you can send
   * multicast UDP to make these work. NOTE: Some CI servers (like Jenkins
   * slaves) might not allow multicast for some reason
   * 
   * @throws IOException
   * @throws InterruptedException - to support the Thread sleep function
   */
  @BeforeClass
  public static void startupTheGear() throws IOException, InterruptedException {
    gen = new ProbeGenerator("230.0.0.1", 4003);
  
    readTargetXMLFiles();
  
    startResponder();
    startListener();
  
    Thread.sleep(2000); // wait 2 seconds for everything to settle
  
  }

  /**
   * Turn off the test harness processes.
   */
  @AfterClass
  public static void tearDown() {
    server.stop();
    Responder.stopResponder();
    gen.close();
  }

  @Test
  public void testHelloWorldService() {
    String responseMsg = target.path("helloworld").get(String.class);
    assertEquals("Hello World", responseMsg);

  }

  @Test
  public void testNakedProbeJSON() throws UnsupportedPayloadType, IOException, InterruptedException {
    Probe probe = new Probe(ProbeWrapper.JSON);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");
    gen.sendProbe(probe); // Send the naked probe
    Thread.sleep(1000); // let the responder process the message and post back
                        // to the listener
    String responseMsg = target.path("listener/responses").get(String.class);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);

    assertTrue(responseMsg.equals(nakedProbeJSONResponseFromListener));
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

  @Test
  public void testNakedProbeXML() throws UnsupportedPayloadType, IOException, InterruptedException {
    Probe probe = new Probe(ProbeWrapper.XML);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");
    gen.sendProbe(probe); // Send the naked probe
    Thread.sleep(1000); // let the responder process the message and post back
                        // to the listener
    String responseMsg = target.path("listener/responses").get(String.class);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);

    assertTrue(responseMsg.equals(nakedProbeXMLResponseFromListener));
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

}
