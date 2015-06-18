package net.di2e.rtsd.ProbeGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeGenerator;
import ws.argo.probe.UnsupportedPayloadType;

public class ProbeGeneratorTest {

	static ProbeGenerator gen = null;
	
	public static String targetXML = "";
	public static String targetNakedProbeXML = "";
	
	@BeforeClass
	public static void setupProbeGenerator() throws IOException {
		gen = new ProbeGenerator("230.0.0.1", 4003);
		
		readTargetXMLFiles();
	}
	
	/**
	 * reads in the test payload text to check responses against.
	 * better then putting it in the source code.
	 * @throws IOException
	 */
	private static void readTargetXMLFiles() throws IOException {
		// Read the completely filled out probe test file for comparison
		assertNotNull("targetProbeXML.xml file missing",   ProbeGeneratorTest.class.getResource("/targetProbeXML.xml"));
		try (InputStream is = ProbeGeneratorTest.class.getResourceAsStream("/targetProbeXML.xml")) {
			targetXML = IOUtils.toString(is, "UTF-8");
		}
	
		// Read the naked (minimally) filled out probe test file for comparison
		assertNotNull("targetNakedProbeXML.xml file missing",   ProbeGeneratorTest.class.getResource("/targetNakedProbeXML.xml"));
		try (InputStream is = ProbeGeneratorTest.class.getResourceAsStream("/targetNakedProbeXML.xml")) {
			targetNakedProbeXML = IOUtils.toString(is, "UTF-8");
		}
	}

	
	@Test(expected=UnsupportedPayloadType.class)
	public void testBadPayloadTypesWithNull() throws UnsupportedPayloadType {
		new Probe(null);
	}
	
	@Test(expected=UnsupportedPayloadType.class)
	public void testBadPayloadTypesWithEmptyString() throws UnsupportedPayloadType {
		new Probe("");
	}
	
	@Test(expected=UnsupportedPayloadType.class)
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
	
	@Test(expected=MalformedURLException.class)
	public void testAddingBadSchemeRespondToURL() throws UnsupportedPayloadType, MalformedURLException {
		Probe probe = new Probe(Probe.XML);
		
		probe.addRespondToURL("bad", "ftp://this is a bad url");
	}
	
	@Test(expected=MalformedURLException.class)
	public void testAddingBadRespondToURL() throws UnsupportedPayloadType, MalformedURLException {
		Probe probe = new Probe(Probe.XML);
		
		probe.addRespondToURL("bad", "http://234.23,23:90/xxx/xxx");
	}
	
	@Test
	public void testLocalhostRespondToURL() throws MalformedURLException, UnsupportedPayloadType {
		Probe probe = new Probe(Probe.XML);
		
		probe.addRespondToURL("localhost", "http://localhost:9998/xxx/xxx");
		
	}
	
	@Test 
	public void testWirelineFormatOfFullyPackedProbe() throws UnsupportedPayloadType, JAXBException, MalformedURLException {
		Probe probe = new Probe(Probe.XML);
		
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
	public void testProbeGeneratorAddingRepsondToURLinXML() throws IOException, UnsupportedPayloadType {
		Probe probe = new Probe(Probe.XML);
		
		probe.addRespondToURL("internal", "http://192.168.0.100:8080/AsynchListener/api/responseHandler/probeResponse");
		
		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");
	}
	
	@Test
	public void testSendingNakedProbe() throws IOException, UnsupportedPayloadType {
		Probe probe = new Probe(Probe.JSON);
		
		probe.addRespondToURL("internal", "http://1.1.1.1:8080/AsynchListener/api/responseHandler/probeResponse");
		// No specified service contract IDs implies "all"
		
		gen.sendProbe(probe);
	}
	
	@Test
	public void testSendingProbeWithJSONProbeFormat() throws IOException, UnsupportedPayloadType {
		Probe probe = new Probe(Probe.JSON);
		
		probe.addRespondToURL("internal", "http://1.1.1.1:8080/AsynchListener/api/responseHandler/probeResponse");
		probe.addRespondToURL("external", "http://1.1.1.1:80/AsynchListener/api/responseHandler/probeResponse");
		// No specified service contract IDs implies "all"
		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");	
		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");
		
		probe.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925d");
		probe.addServiceInstanceID("uuid:03d55093-a954-4667-b682-8116c417925d");
		
		gen.sendProbe(probe);
	}
	
	
	@After
	public void closeProbeGenerator() {
		gen.close();
	}
	
}
