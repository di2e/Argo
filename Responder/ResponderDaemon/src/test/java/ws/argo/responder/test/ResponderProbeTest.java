package ws.argo.responder.test;

import java.io.IOException;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import ws.argo.probe.ProbeGenerator;
import ws.argo.probe.ProbeGeneratorException;
import ws.argo.probe.ProbeGeneratorFactory;
import ws.argo.responder.Responder;
import ws.argo.responder.ResponderConfigException;
import ws.argo.responder.ResponderOperationException;
import ws.argo.responder.test.listener.ResponseListener;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public abstract class ResponderProbeTest {

  static Responder          responder;
  private static HttpServer server;
  static WebResource        target;
  private static Thread     responderThread;
  static ProbeGenerator     gen = null;

  // /**
  // * reads in the test payload text to check responses against. better then
  // * putting it in the source code.
  // *
  // * @throws IOException if the resource is missing
  // */
  // protected void readTargetXMLFiles() throws IOException {
  // }

  private static void startListener() throws IOException {
    server = ResponseListener.startServer();
    target = Client.create().resource(ResponseListener.BASE_URI);
  }

  private static void startResponder() throws ResponderConfigException {

    String configFileProp = System.getProperty("configFile");
    System.out.println("****** Testing configFile = " + configFileProp);
    final String[] args = { "-pf", configFileProp };

    responder = Responder.initialize(args);

    responderThread = new Thread("Argo Responder") {
      public void run() {
        try {
          responder.run();

          System.out.println("Argo Responder ended");
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
   * @throws IOException if something goes wrong starting the responder or
   *           listener
   * @throws InterruptedException - to support the Thread sleep function
   * @throws ResponderConfigException if there is some issue in the Responder
   *           configuration
   * @throws ProbeGeneratorException if something goes wrong with the Probe
   *           Generator
   */
  @BeforeClass
  public static void startupTheGear() throws IOException, InterruptedException, ResponderConfigException, ProbeGeneratorException {
    gen = ProbeGeneratorFactory.createMulticastProbeGenerator();
        
    startResponder();
    startListener();

    Thread.sleep(1000); // wait a seconds for everything to settle

  }

  /**
   * Turn off the test harness processes.
   * 
   * @throws InterruptedException - to support the Thread sleep function
   * @throws ProbeGeneratorException if some problem occurred closing the transport
   */
  @AfterClass
  public static void tearDown() throws InterruptedException, ProbeGeneratorException {

    responder.stopResponder();
    gen.close();
    server.stop();
  }

}
