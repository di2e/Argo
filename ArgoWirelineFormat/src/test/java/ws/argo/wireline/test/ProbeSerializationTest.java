package ws.argo.wireline.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.argo.wireline.probe.ProbeParseException;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.probe.XMLSerializer;

/**
 * This test class will test all of the probe serializations to make sure that
 * the probe is marshalled and unmarshalled into text format correctly.
 * 
 * <p>
 * Look at src/test/resources for the text formats that are used for the tests.
 * 
 * @author jmsimpson
 *
 */
public class ProbeSerializationTest {

  private static String testNakedProbePayload;
  private static String testProbePayload;
  private static String corruptProbePayload1;
  private static String testProbePayloadWithTypos;
  private static String exemplarNakedProbePayload;
  private static String exemplarFullProbePayload;

  @BeforeClass
  public static void setupProbeSender() throws IOException {
    readXMLFiles();
  }

  private static void readXMLFiles() throws IOException {
    // Read the completely filled out probe test file for comparison
    assertNotNull("testProbePayload.xml file missing", ProbeSerializationTest.class.getResource("/testProbePayload.xml"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/testProbePayload.xml")) {
      testProbePayload = IOUtils.toString(is, "UTF-8");
    }

    // Read the naked (minimally) filled out probe test file for comparison
    assertNotNull("testNakedProbePayload.xml file missing", ProbeSerializationTest.class.getResource("/testNakedProbePayload.xml"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/testNakedProbePayload.xml")) {
      testNakedProbePayload = IOUtils.toString(is, "UTF-8");
    }

    // Read the naked (minimally) filled out probe test file for comparison
    assertNotNull("corruptPayload1.txt file missing", ProbeSerializationTest.class.getResource("/corruptPayload1.txt"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/corruptPayload1.txt")) {
      corruptProbePayload1 = IOUtils.toString(is, "UTF-8");
    }

    // Read the probe test file that has typos in the xml
    assertNotNull("testProbePayloadWithTypos.txt file missing", ProbeSerializationTest.class.getResource("/testProbePayloadWithTypos.txt"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/testProbePayloadWithTypos.txt")) {
      testProbePayloadWithTypos = IOUtils.toString(is, "UTF-8");
    }

    assertNotNull("exemplarNakedProbePayload.xml file missing", ProbeSerializationTest.class.getResource("/exemplarNakedProbePayload.xml"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/exemplarNakedProbePayload.xml")) {
      exemplarNakedProbePayload = IOUtils.toString(is, "UTF-8");
    }

    assertNotNull("exemplarFullProbePayload.xml file missing", ProbeSerializationTest.class.getResource("/exemplarFullProbePayload.xml"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/exemplarFullProbePayload.xml")) {
      exemplarFullProbePayload = IOUtils.toString(is, "UTF-8");
    }

  }

  @Test(expected = ProbeParseException.class)
  public void testCorruptProbePayload() throws ProbeParseException {

    XMLSerializer serializer = new XMLSerializer();

    serializer.unmarshal(corruptProbePayload1);

  }

  @Test(expected = ProbeParseException.class)
  public void testPayloadWithTypos() throws ProbeParseException {
    XMLSerializer serializer = new XMLSerializer();

    serializer.unmarshal(testProbePayloadWithTypos);

  }

  @Test
  public void testParsingNakedProbe() throws ProbeParseException {

    XMLSerializer serializer = new XMLSerializer();

    ProbeWrapper probe = serializer.unmarshal(testNakedProbePayload);

    assertTrue(probe.isNaked());

  }

  @Test
  public void testParsingProbe() throws ProbeParseException {

    XMLSerializer serializer = new XMLSerializer();

    ProbeWrapper probe = serializer.unmarshal(testProbePayload);

    assertFalse(probe.isNaked());

  }

  @Test
  public void testMarshallingNakedProbe() {
    ProbeWrapper probe = new ProbeWrapper("--ID--");
    probe.setClientId("yomama");
    probe.setRespondToPayloadType(ProbeWrapper.XML);

    XMLSerializer serializer = new XMLSerializer();

    String payload = serializer.marshal(probe);

    assertTrue(payload.equals(exemplarNakedProbePayload));
  }

  @Test
  public void testMarshallingFullProbe() throws ProbeParseException {
    ProbeWrapper probe = new ProbeWrapper("--ID--");
    probe.setClientId("yomama");
    probe.setRespondToPayloadType(ProbeWrapper.XML);
    probe.addRespondToURL("internal", "http://1.1.1.1:8080/AsynchListener/api/responseHandler/probeResponse");
    probe.addRespondToURL("external", "http://2.2.2.2:80/AsynchListener/api/responseHandler/probeResponse");
    probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925a");
    probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925b");
    probe.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925c");
    probe.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925d");

    XMLSerializer serializer = new XMLSerializer();

    String payload = serializer.marshal(probe);

    ProbeWrapper parsedProbe = serializer.unmarshal(payload);
    ProbeWrapper knownGoodProbe = serializer.unmarshal(exemplarFullProbePayload);

    assertTrue(parsedProbe.equals(knownGoodProbe));
    // assertTrue(payload.equals(exemplarFullProbePayload));

  }

  @Test
  public void testEqualityWithSomeNullValues() {

    ProbeWrapper probe1 = new ProbeWrapper("--ID--");
    probe1.setClientId("yomama");
    probe1.setRespondToPayloadType(ProbeWrapper.XML);
    probe1.addRespondToURL(null, "http://1.1.1.1:8080/AsynchListener/api/responseHandler/probeResponse");
    probe1.addRespondToURL("external", "http://2.2.2.2:80/AsynchListener/api/responseHandler/probeResponse");
    probe1.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925a");
    probe1.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925b");
    probe1.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925c");
    probe1.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925d");

    ProbeWrapper probe2 = new ProbeWrapper("--ID--");
    probe2.setClientId("yomama");
    probe2.setRespondToPayloadType(ProbeWrapper.XML);
    probe2.addRespondToURL("internal", "http://1.1.1.1:8080/AsynchListener/api/responseHandler/probeResponse");
    probe2.addRespondToURL(null, "http://2.2.2.2:80/AsynchListener/api/responseHandler/probeResponse");
    probe2.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925a");
    probe2.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925b");
    probe2.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925c");
    probe2.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925d");

    assertFalse(probe1.equals(probe2));

  }

  @Test
  public void testEqualityWithSomeLeadingAndTrailingSpaces() {

    ProbeWrapper probe1 = new ProbeWrapper("--ID--");
    probe1.setClientId("  yomama");
    probe1.setRespondToPayloadType(ProbeWrapper.XML);
    probe1.addRespondToURL("internal", "http://1.1.1.1:8080/AsynchListener/api/responseHandler/probeResponse");
    probe1.addRespondToURL("external", "http://2.2.2.2:80/AsynchListener/api/responseHandler/probeResponse  ");
    probe1.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925a  ");
    probe1.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925b");
    probe1.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925c");
    probe1.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925d");

    ProbeWrapper probe2 = new ProbeWrapper("--ID--");
    probe2.setClientId("yomama  ");
    probe2.setRespondToPayloadType(ProbeWrapper.XML);
    probe2.addRespondToURL("internal", "  http://1.1.1.1:8080/AsynchListener/api/responseHandler/probeResponse");
    probe2.addRespondToURL("external", "http://2.2.2.2:80/AsynchListener/api/responseHandler/probeResponse");
    probe2.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925a");
    probe2.addServiceContractID("  uuid:03d55093-a954-4667-b682-8116c417925b");
    probe2.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925c");
    probe2.addServiceInstanceID("  uuid:03d55093-a954-4667-b682-8116c417925d");

    assertFalse(probe1.equals(probe2));

  }

}
