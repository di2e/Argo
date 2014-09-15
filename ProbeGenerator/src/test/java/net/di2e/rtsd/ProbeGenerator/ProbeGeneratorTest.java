package net.di2e.rtsd.ProbeGenerator;

import java.io.IOException;
import java.net.SocketException;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

public class ProbeGeneratorTest {

	ProbeGenerator gen = null;
	
	@Before
	public void setupProbeGenerator() throws SocketException {
		gen = new ProbeGenerator("230.0.0.1", 4446);
	}
	
	@Test
	public void testProbeGeneratorXML() throws IOException {
		Probe probe = new Probe("http://192.168.0.18:8080/AsynchListener/api/responseHandler/probeResponse", Probe.XML);
		
		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");
		
		gen.sendProbe(probe);
	}
	
	@Test
	public void testProbeGeneratorJSON() throws IOException {
		Probe probe = new Probe("http://192.168.0.18:8080/AsynchListener/api/responseHandler/probeResponse", Probe.JSON);
		
		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");	
		
		gen.sendProbe(probe);
	}
	
	@Test
	public void testProbeGeneratorAllService() throws IOException {
		Probe probe = new Probe("http://192.168.0.18:8080/AsynchListener/api/responseHandler/probeResponse", Probe.JSON);
		
//		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");	
		
		gen.sendProbe(probe);
	}
	
	@After
	public void closeProbeGenerator() {
		gen.close();
	}
	
}
