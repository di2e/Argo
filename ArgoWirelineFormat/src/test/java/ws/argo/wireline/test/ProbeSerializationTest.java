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

public class ProbeSerializationTest {

  private static String testNakedProbePayload;
  private static String testProbePayload;
  private static String corruptProbePayload1;
  private static String testProbePayloadWithTypos;
  private static String exemplarNakedProbePayload;
  private static String exemplarFullProbePayload;

  @BeforeClass
  public static void setupProbeGenerator() throws IOException {
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

  @Test(expected=ProbeParseException.class)
  public void testCorruptProbePayload() throws ProbeParseException {
    
    XMLSerializer serializer = new XMLSerializer();

    serializer.unmarshal(corruptProbePayload1);
   
  }
  
  @Test(expected=ProbeParseException.class)
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
  public void testMarshallingFullProbe() {
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
    
    assertTrue(payload.equals(exemplarFullProbePayload));
    
  }

  // @SuppressWarnings("unused")
  // @Test
  // public void testBasicResponseXMLPayload() {

  // ResponseWrapper response = new ResponseWrapper("this is a probe id");
  //
  // ServiceWrapper service = new ServiceWrapper("this is a service id");
  // service.setConsumability(ServiceWrapper.HUMAN_CONSUMABLE);
  // service.setContractDescription("this is a contract description");
  // service.setDescription("this is a description");
  // service.setServiceContractID("this is a service contract id");
  // service.setServiceName("this is a service name");
  // service.setTtl(0);
  //
  // service.addAccessPoint("label 1", "some IP address", "some port",
  // "some URL", "some datatype", "some data");
  // service.addAccessPoint("label 2", "some IP address", "some port",
  // "some URL", "some datatype", "some data");
  //
  // response.addResponse(service);
  //
  // XMLSerializer serializer = new XMLSerializer(response);
  //
  //
  //
  // String responseString = serializer.marshal();

  // Do some assertion here

  // }

  // @SuppressWarnings("unused")
  // @Test
  // public void testBasicResponseJSONPayload() {

  // ResponseWrapper response = new ResponseWrapper("this is a probe id");
  //
  // ServiceWrapper service = new ServiceWrapper("this is a service id");
  // service.setConsumability(ServiceWrapper.HUMAN_CONSUMABLE);
  // service.setContractDescription("this is a contract description");
  // service.setDescription("this is a description");
  // service.setServiceContractID("this is a service contract id");
  // service.setServiceName("this is a service name");
  // service.setTtl(0);
  //
  // service.addAccessPoint("label 1", "some IP address", "some port",
  // "some URL", "some datatype", "some data");
  // service.addAccessPoint("label 2", "some IP address", "some port",
  // "some URL", "some datatype", "some data");
  //
  // response.addResponse(service);
  //
  // JSONSerializer serializer = new JSONSerializer(response);
  //
  //
  // String responseString = serializer.serialize();
  //
  // Do some assertion here

  // }

}
