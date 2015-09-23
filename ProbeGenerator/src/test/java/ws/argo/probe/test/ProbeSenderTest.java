package ws.argo.probe.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSender;
import ws.argo.probe.ProbeSenderException;
import ws.argo.probe.ProbeSenderFactory;
import ws.argo.probe.UnsupportedPayloadType;
import ws.argo.probe.transport.TransportConfigException;
import ws.argo.wireline.probe.ProbeWrapper;

/**
 * Test the ProbeSender.
 * 
 * @author jmsimpson
 *
 */
public class ProbeSenderTest {

  static ProbeSender mcSender = null;

  static String targetXML           = "";
  static String targetNakedProbeXML = "";

  /**
   * Start up a ProbeSender for the harness and read in the XML files used in
   * the evaluation.
   * 
   * @throws IOException if something goes wrong reading the XML files
   * @throws TransportConfigException if there is a problem with the config
   *           (shouldn't be here)
   */
  @BeforeClass
  public static void setupProbeSender() throws ProbeSenderException, TransportConfigException, IOException {
    mcSender = ProbeSenderFactory.createMulticastProbeSender();

    readTargetXMLFiles();
  }

  /**
   * reads in the test payload text to check responses against. better then
   * putting it in the source code.
   * 
   * @throws IOException if something goes wrong reading the XML files
   */
  private static void readTargetXMLFiles() throws IOException {
    // Read the completely filled out probe test file for comparison
    assertNotNull("targetProbeXML.xml file missing", ProbeSenderTest.class.getResource("/targetProbeXML.xml"));
    try (InputStream is = ProbeSenderTest.class.getResourceAsStream("/targetProbeXML.xml")) {
      targetXML = IOUtils.toString(is, "UTF-8");
    }

    // Read the naked (minimally) filled out probe test file for comparison
    assertNotNull("targetNakedProbeXML.xml file missing", ProbeSenderTest.class.getResource("/targetNakedProbeXML.xml"));
    try (InputStream is = ProbeSenderTest.class.getResourceAsStream("/targetNakedProbeXML.xml")) {
      targetNakedProbeXML = IOUtils.toString(is, "UTF-8");
    }
  }

  @Test(expected = UnsupportedPayloadType.class)
  public void testBadPayloadTypesWithEmptyString() throws UnsupportedPayloadType {
    new Probe("");
  }

  @Test(expected = UnsupportedPayloadType.class)
  public void testBadPayloadTypesWithEmptyNotXMLorJSON() throws UnsupportedPayloadType {
    new Probe("yomama");
  }

  @Test
  public void testCreatingGoodProbeWithJSON() throws UnsupportedPayloadType {
    new Probe(Probe.JSON);
  }

  @Test
  public void testCreatingGoodProbeWithXML() throws UnsupportedPayloadType {
    new Probe(Probe.XML);
  }

  @Test
  public void testWirelineFormatOfNakedProbe() throws UnsupportedPayloadType, JAXBException {
    Probe probe = new Probe(Probe.XML);

    String xml = probe.asXML();

    String testXML = targetNakedProbeXML.replaceAll("--ID--", probe.getProbeID());

    assertEquals(testXML, xml);
  }

  @Test(expected = MalformedURLException.class)
  public void testAddingBadSchemeRespondToURL() throws UnsupportedPayloadType, MalformedURLException {
    Probe probe = new Probe(ProbeWrapper.XML);

    probe.addRespondToURL("bad", "ftp://this is a bad url");
  }

  @Test(expected = MalformedURLException.class)
  public void testAddingBadRespondToURL() throws UnsupportedPayloadType, MalformedURLException {
    Probe probe = new Probe(ProbeWrapper.XML);

    probe.addRespondToURL("bad", "http://234.23,23:90/xxx/xxx");
  }

  @Test
  public void testLocalhostRespondToURL() throws MalformedURLException, UnsupportedPayloadType {
    Probe probe = new Probe(ProbeWrapper.XML);

    probe.addRespondToURL("localhost", "http://localhost:9998/xxx/xxx");

  }

  @Test
  public void testWirelineFormatOfFullyPackedProbe() throws UnsupportedPayloadType, JAXBException, MalformedURLException {
    Probe probe = new Probe(ProbeWrapper.XML);

    probe.setClientID("yomama");

    probe.addRespondToURL("internal", "http://1.1.1.1:8080/AsynchListener/api/responseHandler/probeResponse");
    probe.addRespondToURL("external", "http://1.1.1.1:80/AsynchListener/api/responseHandler/probeResponse");
    // No specified service contract IDs implies "all"
    probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");
    probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");

    probe.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925d");
    probe.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925d");

    String testXML = targetXML.replaceAll("--ID--", probe.getProbeID());

    String xml = probe.asXML();

    assertEquals(testXML, xml);
  }

  @Test
  public void testProbeSenderAddingRepsondToURLinXML() throws IOException, UnsupportedPayloadType {
    Probe probe = new Probe(ProbeWrapper.XML);

    probe.addRespondToURL("internal", "http://192.168.0.100:8080/AsynchListener/api/responseHandler/probeResponse");

    probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");
  }

  @Test
  public void testSendingNakedProbe() throws UnsupportedPayloadType, ProbeSenderException, MalformedURLException {
    Probe probe = new Probe(Probe.JSON);

    probe.addRespondToURL("internal", "http://1.1.1.1:8080/AsynchListener/api/responseHandler/probeResponse");
    // No specified service contract IDs implies "all"

    mcSender.sendProbe(probe);
  }

  @Test
  public void testSendingProbeWithJSONProbeFormat() throws UnsupportedPayloadType, ProbeSenderException, MalformedURLException {
    Probe probe = new Probe(Probe.JSON);

    probe.addRespondToURL("internal", "http://1.1.1.1:8080/AsynchListener/api/responseHandler/probeResponse");
    probe.addRespondToURL("external", "http://1.1.1.1:80/AsynchListener/api/responseHandler/probeResponse");
    // No specified service contract IDs implies "all"
    probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");
    probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");

    probe.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925d");
    probe.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925d");

    mcSender.sendProbe(probe);
  }

  @AfterClass
  public static void closeProbeSender() throws ProbeSenderException {
    mcSender.close();
  }

}
