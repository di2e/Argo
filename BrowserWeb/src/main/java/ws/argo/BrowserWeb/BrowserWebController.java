package ws.argo.BrowserWeb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import ws.argo.ProbeGenerator.Probe;
import ws.argo.ProbeGenerator.ProbeGenerator;


@Path("controller")
public class BrowserWebController {
	
	private static final String PROBE_GENERATOR_PROPERTIES_FILE = "probeGenerator.props";
	private static final String DEFAULT_PROBE_RESPONSE_URL_PATH = "/AsynchListener/api/responseHandler/probeResponse";
	private static final String DEFAULT_RESPONSES_URL_PATH = "/AsynchListener/api/responseHandler/responses";
	private static final String DEFAULT_CLEAR_CACHE_URL_PATH = "/AsynchListener/api/responseHandler/clearCache";
	private static final String DEFAULT_MULTICAST_GROUP_ADDR = "230.0.0.1";
	private static final Integer DEFAULT_MULTICAST_PORT = 4446;
	
	protected CloseableHttpClient httpClient = null;

	
	@GET
	@Path("/testService")
	public String testService() {
		return "this is a test service for the controller.  yea.";
	}

	@GET
	@Path("/launchProbe")
	@Produces("application/txt")
	public String launchProbe() throws IOException {
		
		Properties pgProps = getPropeGeneratorProps();
		
		String multicastGroupAddr = pgProps.getProperty("multicastGroupAddr", DEFAULT_MULTICAST_GROUP_ADDR);
		String multicastPortString = pgProps.getProperty("multicastPort", DEFAULT_MULTICAST_PORT.toString());
		String listenerIPAddress = pgProps.getProperty("listenerIPAddress");
		String listenerPort = pgProps.getProperty("listenerPort");
		String listenerURLPath = pgProps.getProperty("listenerProbeResponseURLPath", DEFAULT_PROBE_RESPONSE_URL_PATH);
		
		Integer multicastPort = Integer.parseInt(multicastPortString);
		
		ProbeGenerator gen = new ProbeGenerator(multicastGroupAddr, multicastPort);
		
		Probe probe = new Probe("http://"+listenerIPAddress+":"+listenerPort+listenerURLPath, Probe.JSON);
		
		// No specified service contract IDs implies "all"
		
		gen.sendProbe(probe);
		gen.close();
		
		return "Probe launched successfully";

	}
	
	@GET
	@Path("/responses")
	@Produces("application/json")
	public String getResponses() throws UnknownHostException {
		Properties pgProps = getPropeGeneratorProps();
		
		String listenerIPAddress = pgProps.getProperty("listenerIPAddress");
		String listenerPort = pgProps.getProperty("listenerPort");
		String listenerReponsesURLPath = pgProps.getProperty("listenerReponsesURLPath", DEFAULT_RESPONSES_URL_PATH);
		
		String response = restGetCall("http://"+listenerIPAddress+":"+listenerPort+listenerReponsesURLPath);
		
		return response;
	}
	
	@GET
	@Path("/clearCache")
	@Produces("application/json")
	public String clearCache() throws UnknownHostException {
		Properties pgProps = getPropeGeneratorProps();
		
		String listenerIPAddress = pgProps.getProperty("listenerIPAddress");
		String listenerPort = pgProps.getProperty("listenerPort");
		String listenerClearCacheURLPath = pgProps.getProperty("listenerClearCacheURLPath", DEFAULT_CLEAR_CACHE_URL_PATH);
		
		String response = restGetCall("http://"+listenerIPAddress+":"+listenerPort+listenerClearCacheURLPath);
		
		return response;
	}
	
	private Properties getPropeGeneratorProps() throws UnknownHostException {
		
		InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(PROBE_GENERATOR_PROPERTIES_FILE);
		
		Properties pgProps = new Properties();
		
		if (in != null) {
			try {
				pgProps.load(in);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// Should log the props file issue
				setDefaultProbeGeneratorProperties(pgProps);
			}
			
		} 
		
		if (pgProps.getProperty("listenerIPAddress","").equals("")) {
			String hostIPAddr = InetAddress.getLocalHost().getHostAddress();
			pgProps.put("listenerIPAddress", hostIPAddr);
		}
		
		return pgProps;
	}
	
	private void setDefaultProbeGeneratorProperties(Properties props) {
		props.put("multicastPort", DEFAULT_MULTICAST_PORT);
		props.put("multicastGroupAddr", DEFAULT_MULTICAST_GROUP_ADDR);
		props.put("listenerIPAddress", "");
		props.put("listenerURLPath", DEFAULT_PROBE_RESPONSE_URL_PATH);
	}
	
	private String restGetCall(String url) {
		String response = "";
		
		try {

			HttpGet getRequest = new HttpGet(url);

			CloseableHttpResponse httpResponse = getHttpClient().execute(getRequest);
			
			try {
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				if (statusCode > 300) {
					throw new RuntimeException("Failed : HTTP error code : "
							+ httpResponse.getStatusLine().getStatusCode());
				}
	
				if (statusCode != 204) {
					BufferedReader br = new BufferedReader(new InputStreamReader(
						(httpResponse.getEntity().getContent())));
					
					StringBuffer buf = new StringBuffer();
	
					String output;
					//System.out.println("Output from Listener .... \n");
					while ((output = br.readLine()) != null) {
						buf.append(output);
					}
					
					response = buf.toString();
				}
			} finally {
				httpClient.close();
			}
			
//			System.out.println("Response payload sent successfully to respondTo address.");

		} catch (MalformedURLException e) {
			System.out.println("MalformedURLException occured\nThe respondTo URL was a no good.  respondTo URL is: "+url);
//			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("An IOException occured: the error message is - "+e.getMessage());
//			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Some error occured: the error message is - "+e.getMessage());
//			e.printStackTrace();
		}
		
		return response;
	}
	
	CloseableHttpClient getHttpClient() {
		
		if (httpClient == null) {
			httpClient = HttpClients.createDefault();
		}
		
		return httpClient;
		
	}

	
}
