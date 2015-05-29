package ws.argo.responder.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeGenerator;
import ws.argo.probe.UnsupportedPayloadType;
import ws.argo.responder.test.listener.ResponseListener;
import ws.argo.responder.Responder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class ResponderProbeTest {

  private static HttpServer  server;
  private static WebResource target;
  private static Thread      responderThread;
  static ProbeGenerator      gen = null;

  private static String      nakedProbeResponseFromListener;

  /**
   * reads in the test payload text to check responses against. better then
   * putting it in the source code.
   * 
   * @throws IOException
   *           if the resource is missing
   */
  private static void readTargetXMLFiles() throws IOException {
    // Read the completely filled out probe test file for comparison
    assertNotNull("targetProbeXML.xml file missing", ResponderProbeTest.class.getResource("/nakedProbeResponseFromListener.json"));
    try (InputStream is = ResponderProbeTest.class
        .getResourceAsStream("/nakedProbeResponseFromListener.json")) {
      nakedProbeResponseFromListener = IOUtils.toString(is, "UTF-8");
    }

    // Read the naked (minimally) filled out probe test file for comparison
    // assertNotNull("targetNakedProbeXML.xml file missing",
    // ProbeGeneratorTest.class.getResource("/targetNakedProbeXML.xml"));
    // try (InputStream is =
    // ProbeGeneratorTest.class.getResourceAsStream("/targetNakedProbeXML.xml"))
    // {
    // targetNakedProbeXML = IOUtils.toString(is, "UTF-8");
    // }
  }

  private static void startResponder() {

    String configFileProp = System.getProperty("configFile");
    System.out.println("****** Testing configFile = " + configFileProp);
    final String[] args = { "-pf", configFileProp };

    responderThread = new Thread() {
      public void run() {
        try {
          Responder.main(args);
        } catch (ClassNotFoundException | IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    };
    responderThread.start();
  }

  private static void startListener() throws IOException {
    server = ResponseListener.startServer();

    target = Client.create().resource(ResponseListener.BASE_URI);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    server.stop();
  }

  @BeforeClass
  public static void startupTheGear() throws IOException, InterruptedException {
    gen = new ProbeGenerator("230.0.0.1", 4003);

    readTargetXMLFiles();

    startResponder();
    startListener();

    Thread.sleep(2000); // wait 2 seconds for everything to settle

  }

  @Test
  public void testHelloWorldService() {
    String responseMsg = target.path("helloworld").get(String.class);
    assertEquals("Hello World", responseMsg);

  }

  @Test
  public void testNakedProbeJSON() throws UnsupportedPayloadType, IOException, InterruptedException {
    Probe probe = new Probe(Probe.JSON);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");
    gen.sendProbe(probe); // Send the naked probe
    Thread.sleep(1000); // let the responder process the message and post back
                        // to the listener
    String responseMsg = target.path("listener/responses").get(String.class);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);

    assertEquals(nakedProbeResponseFromListener.length(), responseMsg.length());
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

  @Test
  public void testNakedProbeXML() throws UnsupportedPayloadType, IOException, InterruptedException {
    Probe probe = new Probe(Probe.XML);
    probe.addRespondToURL("", "http://localhost:9998/listener/probeResponse");
    gen.sendProbe(probe); // Send the naked probe
    Thread.sleep(1000); // let the responder process the message and post back
                        // to the listener
    String responseMsg = target.path("listener/responses").get(String.class);

    String cacheClearedMsg = target.path("listener/clearCache").get(String.class);

    assertEquals(nakedProbeResponseFromListener.length(), responseMsg.length());
    assertEquals("Cleared Cache", cacheClearedMsg);
  }

}
