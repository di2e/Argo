package net.di2e.rtsd.BrowserWeb;

import java.io.IOException;
import java.net.InetAddress;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import net.di2e.rtsd.ProbeGenerator.Probe;
import net.di2e.rtsd.ProbeGenerator.ProbeGenerator;


@Path("controller")
public class BrowserWebController {
	
	@GET
	@Path("/testService")
	public String testService() {
		return "this is a test service for the controller.  yea.";
	}

	@GET
	@Path("/launchProbe")
	@Produces("application/txt")
	public String getResponses() throws IOException {
		ProbeGenerator gen = new ProbeGenerator("230.0.0.1", 4446);
		
		String hostIPAddr = InetAddress.getLocalHost().getHostAddress();
		Probe probe = new Probe("http://"+hostIPAddr+":8080/AsynchListener/api/responseHandler/probeResponse", Probe.JSON);
		
		// No specified service contract IDs implies "all"
		
		gen.sendProbe(probe);
		gen.close();
		
		return "Probe launched successfully";

	}
	

	
}
