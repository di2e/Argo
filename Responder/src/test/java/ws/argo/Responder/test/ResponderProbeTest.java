package ws.argo.Responder.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.argo.ProbeGenerator.Probe;
import ws.argo.ProbeGenerator.ProbeGenerator;
import ws.argo.ProbeGenerator.UnsupportedPayloadType;
import ws.argo.Responder.Responder;
import ws.argo.Responder.test.listener.ResponseListener;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class ResponderProbeTest {

	private static HttpServer server;
	private static WebResource target;
	private static Thread responderThread;
	static ProbeGenerator gen = null;
	
	private static String nakedProbeResponseFromListener;

	/**
	 * reads in the test payload text to check responses against.
	 * better then putting it in the source code.
	 * @throws IOException
	 */
	private static void readTargetXMLFiles() throws IOException {
		// Read the completely filled out probe test file for comparison
		assertNotNull("targetProbeXML.xml file missing",   ResponderProbeTest.class.getResource("/nakedProbeResponseFromListener.json"));
		try (InputStream is = ResponderProbeTest.class.getResourceAsStream("/nakedProbeResponseFromListener.json")) {
			nakedProbeResponseFromListener = IOUtils.toString(is, "UTF-8");
		}
	
		// Read the naked (minimally) filled out probe test file for comparison
//		assertNotNull("targetNakedProbeXML.xml file missing",   ProbeGeneratorTest.class.getResource("/targetNakedProbeXML.xml"));
//		try (InputStream is = ProbeGeneratorTest.class.getResourceAsStream("/targetNakedProbeXML.xml")) {
//			targetNakedProbeXML = IOUtils.toString(is, "UTF-8");
//		}
	}

	private static void startResponder() {

		String configFileProp = System.getProperty("configFile");
		System.out.println("****** Testing configFile = "+configFileProp);
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
        
        Client c = Client.create();
        target = c.resource(ResponseListener.BASE_URI);
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
		
		Thread.sleep(2000); //wait 2 seconds for everything to settle

	}
	
	@Test
	public void testHelloWorldService()
	{
        String responseMsg = target.path("helloworld").get(String.class);
        assertEquals("Hello World", responseMsg);
		
	}
	
	@Test
	public void testNakedProbeJSON() throws UnsupportedPayloadType, IOException, InterruptedException {
		Probe p = new Probe(Probe.JSON);
		p.addRespondToURL("", "http://localhost:9998/listener/probeResponse");
		gen.sendProbe(p); //Send the naked probe
		Thread.sleep(1000); //let the responder process the message and post back to the listener
        String responseMsg = target.path("listener/responses").get(String.class);
 
        String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
		
        assertEquals(nakedProbeResponseFromListener.length(), responseMsg.length());
        assertEquals("Cleared Cache", cacheClearedMsg);
	}

	@Test
	public void testNakedProbeXML() throws UnsupportedPayloadType, IOException, InterruptedException {
		Probe p = new Probe(Probe.XML);
		p.addRespondToURL("", "http://localhost:9998/listener/probeResponse");
		gen.sendProbe(p); //Send the naked probe
		Thread.sleep(1000); //let the responder process the message and post back to the listener
        String responseMsg = target.path("listener/responses").get(String.class);
 
        String cacheClearedMsg = target.path("listener/clearCache").get(String.class);
		
        assertEquals(nakedProbeResponseFromListener.length(), responseMsg.length());
        assertEquals("Cleared Cache", cacheClearedMsg);
	}

}
