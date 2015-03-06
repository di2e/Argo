package net.di2e.rtsd.ProbeGenerator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import ws.argo.ProbeGenerator.Probe;
import ws.argo.ProbeGenerator.ProbeGenerator;

public class ProbeGeneratorTest {

	ProbeGenerator gen = null;
	
	@Before
	public void setupProbeGenerator() throws IOException {
		gen = new ProbeGenerator("230.0.0.1", 4003);
	}
	
	@Test
	public void testProbeGeneratorXML() throws IOException {
		String hostIPAddr = InetAddress.getLocalHost().getHostAddress();
		Probe probe = new Probe("http://"+hostIPAddr+":8080/AsynchListener/api/responseHandler/probeResponse", Probe.XML);
		
		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");
		
		gen.sendProbe(probe);
	}
	
	@Test
	public void testProbeGeneratorJSON() throws IOException {
		
//		10.12.128.136
		
		String hostIPAddr = InetAddress.getLocalHost().getHostAddress();
		
		Probe probe = new Probe("http://"+hostIPAddr+":8080/AsynchListener/api/responseHandler/probeResponse", Probe.JSON);
		
		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");	
		
		System.out.println(probe.asJSON());
	}
	
	@Test
	public void testProbeGeneratorAllService() throws IOException {
		String hostIPAddr = InetAddress.getLocalHost().getHostAddress();
		Probe probe = new Probe("http://"+hostIPAddr+":8080/AsynchListener/api/responseHandler/probeResponse", Probe.JSON);
		
		// No specified service contract IDs implies "all"
//		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");	
		
		gen.sendProbe(probe);
	}
	
	@Test
	public void testProbeGeneratorJSONProbeFormat() throws IOException {
		String hostIPAddr = InetAddress.getLocalHost().getHostAddress();
		Probe probe = new Probe("http://"+hostIPAddr+":8080/AsynchListener/api/responseHandler/probeResponse", Probe.JSON);
		
		// No specified service contract IDs implies "all"
//		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");	
		
		gen.sendProbe(probe);
	}
	@After
	public void closeProbeGenerator() {
		gen.close();
	}
	
}
