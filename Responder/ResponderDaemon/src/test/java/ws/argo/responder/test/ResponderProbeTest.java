package ws.argo.responder.test;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import ws.argo.probe.ProbeSender;
import ws.argo.probe.ProbeSenderException;
import ws.argo.probe.ProbeSenderFactory;
import ws.argo.probe.transport.TransportConfigException;
import ws.argo.responder.Responder;
import ws.argo.responder.ResponderConfigException;
import ws.argo.responder.test.listener.ResponseListener;

/**
 * Test Responder. This is an abstract class that provides some boilerplate to
 * the other concrete tests.
 * 
 * @author jmsimpson
 *
 */
public abstract class ResponderProbeTest {

  static Responder          responder;
  private static HttpServer server;
  static WebTarget          target;
  private static Thread     responderThread;
  static ProbeSender        gen = null;

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
    Client client = ClientBuilder.newClient();
    target = client.target(ResponseListener.BASE_URI);
  }

  private static void startResponder() throws ResponderConfigException {

    String configFileProp = System.getProperty("configFile");
    System.out.println("****** Testing configFile = " + configFileProp);
    final String[] args = { "-pf", configFileProp };

    responder = Responder.initialize(args);

    responderThread = new Thread("Argo Responder") {
      public void run() {
        responder.run();
        System.out.println("Argo Responder ended");
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
   * @throws ProbeSenderException if something goes wrong with the Probe
   *           Generator
   * @throws TransportConfigException  if something goes wrong
   */
  @BeforeClass
  public static void startupTheGear() throws IOException, InterruptedException, ResponderConfigException, ProbeSenderException, TransportConfigException {
    gen = ProbeSenderFactory.createMulticastProbeSender();

    startResponder();
    startListener();

    Thread.sleep(1000); // wait a seconds for everything to settle

  }

  /**
   * Turn off the test harness processes.
   * 
   * @throws InterruptedException - to support the Thread sleep function
   * @throws ProbeSenderException if some problem occurred closing the transport
   */
  @AfterClass
  public static void tearDown() throws InterruptedException, ProbeSenderException {

    responder.stopResponder();
    gen.close();
    server.shutdownNow();
  }

}
