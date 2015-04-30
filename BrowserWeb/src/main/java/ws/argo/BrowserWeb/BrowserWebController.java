/*
 * Copyright 2015 Jeff Simpson.
 *
 * Licensed under the MIT License, (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.argo.BrowserWeb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import ws.argo.ProbeGenerator.Probe;
import ws.argo.ProbeGenerator.ProbeGenerator;


/*
 * @author Jeff Simpson
 * @since v0.0.1
 */

@Path("controller")
public class BrowserWebController {
	
	private final static Logger LOGGER = Logger.getLogger(BrowserWebController.class.getName());

	private static final String PROBE_GENERATOR_PROPERTIES_FILE = "argoClient.prop";
	private static final String DEFAULT_PROBE_RESPONSE_URL_PATH = "/AsynchListener/api/responseHandler/probeResponse";
	private static final String DEFAULT_RESPONSES_URL_PATH = "/AsynchListener/api/responseHandler/responses";
	private static final String DEFAULT_CLEAR_CACHE_URL_PATH = "/AsynchListener/api/responseHandler/clearCache";
	private static final String DEFAULT_MULTICAST_GROUP_ADDR = "230.0.0.1";
	private static final Integer DEFAULT_MULTICAST_PORT = 4003;
	private static final String ARGO_HOME_ENV_NAME = "ARGO_HOME";
	private static final String ARGO_HOME_PROPS_PATH = "/config/";
	
	protected CloseableHttpClient httpClient = null;
	protected static Properties clientProps = null;
	
	protected static ArrayList<ProbeRespondToAddress> respondToAddresses = new ArrayList<ProbeRespondToAddress>();

	
	@GET
	@Path("/testService")
	public String testService() {
		return "this is a test service for the controller.  yea.";
	}

	@GET
	@Path("/launchProbe")
	@Produces("application/txt")
	public String launchProbe() throws IOException {
		
		Properties clientProps = getPropeGeneratorProps();
		
		String multicastGroupAddr = clientProps.getProperty("multicastGroupAddr", DEFAULT_MULTICAST_GROUP_ADDR);
		String multicastPortString = clientProps.getProperty("multicastPort", DEFAULT_MULTICAST_PORT.toString());
		
//		String listenerIPAddress = clientProps.getProperty("listenerIPAddress");
//		String listenerPort = clientProps.getProperty("listenerPort");
		String listenerURLPath = clientProps.getProperty("listenerProbeResponseURLPath", DEFAULT_PROBE_RESPONSE_URL_PATH);
		
		Integer multicastPort = Integer.parseInt(multicastPortString);
		
		ProbeGenerator gen = new ProbeGenerator(multicastGroupAddr, multicastPort);

		// loop over the "respond to addresses" specified in the properties file.
		for (ProbeRespondToAddress rta : respondToAddresses) {
			
			Probe probe = new Probe("http://"+rta.respondToAddress+":"+rta.respondToPort+listenerURLPath, Probe.JSON);
			// The following is a "naked" probe - no service contract IDs, etc.
			// No specified service contract IDs implies "all"
			// This will evoke responses from all reachable responders except those configured to "noBrowser"
			gen.sendProbe(probe);
		}
		
		gen.close();
		
		return "Launched "+respondToAddresses.size()+" probe(s) successfully on "+multicastGroupAddr+":"+ multicastPort;

	}
	
	@GET
	@Path("/responses")
	@Produces("application/json")
	public String getResponses() throws UnknownHostException {
		Properties clientProps = getPropeGeneratorProps();
		
		String listenerIPAddress = clientProps.getProperty("listenerIPAddress");
		String listenerPort = clientProps.getProperty("listenerPort");
		String listenerReponsesURLPath = clientProps.getProperty("listenerReponsesURLPath", DEFAULT_RESPONSES_URL_PATH);
		
		String response = restGetCall("http://"+listenerIPAddress+":"+listenerPort+listenerReponsesURLPath);
		
		return response;
	}
	
	@GET
	@Path("/clearCache")
	@Produces("application/json")
	public String clearCache() throws UnknownHostException {
		Properties clientProps = getPropeGeneratorProps();
		
		String listenerIPAddress = clientProps.getProperty("listenerIPAddress");
		String listenerPort = clientProps.getProperty("listenerPort");
		String listenerClearCacheURLPath = clientProps.getProperty("listenerClearCacheURLPath", DEFAULT_CLEAR_CACHE_URL_PATH);
		
		String response = restGetCall("http://"+listenerIPAddress+":"+listenerPort+listenerClearCacheURLPath);
		
		return response;
	}
	
	
	@SuppressWarnings("resource")
	private static InputStream getPropertiesFileInputStream() {
		InputStream is = null;
		
		String ARGO_HOME = System.getenv(ARGO_HOME_ENV_NAME);
		String externPropsFilename = ARGO_HOME+ARGO_HOME_PROPS_PATH+PROBE_GENERATOR_PROPERTIES_FILE;
		
		File file = new File(externPropsFilename);
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			LOGGER.warning("External config file "+externPropsFilename+" not found.");
			LOGGER.warning("Using defaults located in classpath embedded in war file.");
			is = BrowserWebController.class.getClassLoader().getResourceAsStream(PROBE_GENERATOR_PROPERTIES_FILE);
		}
		
		return is;
	}
	
	/*
	 * provides the correct host IP address from a string (propIPAddress).
	 * This method takes the IP addresses from the properties file and then resolves it to be
	 * the correct IP address for the local host to use.
	 * 
	 * The propIPAddress might be empty.  In this case return the correct IP address for the local host
	 * and not the loopback address.  This makes an assumption that if the user omitted the IP address
	 * from the config file, then just use the local host address.
	 * 
	 * This handles the majority of cases where you don't know the IP address but want simple configuration.
	 * It's a good way to default to the local host IP address.
	 * 
	 * @param	propIPAddress the IP address from the properties file
	 * 
	 * @return	the value of the correct string for the local host IP address if the propIPAddress is empty
	 */
	private static String getHostIPAddressFromString(String propIPAddress, String niName) {
		String hostIPAddr = propIPAddress;

		if (propIPAddress.equals("")) {
			// If the listenerIPAddress is blank, then try to get the ip address of the interface
			
			try {
				NetworkInterface ni = null;
				if (niName != null)
					ni = NetworkInterface.getByName(niName);
				if (ni == null) {
					LOGGER.info("Network Interface name not specified or incorrect.  Defaulting to localhost");
					hostIPAddr = InetAddress.getLocalHost().getHostAddress();
				} else {
				
					if (!ni.isLoopback()) {
					    
					    Enumeration ee = ni.getInetAddresses();
					    while (ee.hasMoreElements())
					    {
					        InetAddress i = (InetAddress) ee.nextElement();
					        if (i instanceof Inet4Address) {
					        	hostIPAddr = i.getHostAddress();
					        	break;  // get the first one an get out of the loop
					        }
					    }
					}
				}
			
			} catch (SocketException e) {
				LOGGER.warning("A socket exception occurred.");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				LOGGER.log(Level.WARNING, "Error finding Network Interface", e);
			}
			
		}		
		
		return hostIPAddr;
	}
	
	private static synchronized Properties getPropeGeneratorProps() throws UnknownHostException {
		
		if (clientProps != null)
			return clientProps;
		
		InputStream in = getPropertiesFileInputStream();
		
		clientProps = new Properties();
		
		if (in != null) {
			try {
				clientProps.load(in);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// Should log the props file issue
				setDefaultProbeGeneratorProperties(clientProps);
			}
			
		} 
		
		String niName = clientProps.getProperty("networkInterfaceName");
		if (niName.isEmpty()) niName = null;
		
		// Get the listener IP address (for cache related GUI calls) - usually localhost
		
		String listenerIPAddress = clientProps.getProperty("listenerIPAddress","");
		if (listenerIPAddress.equals("")) {
			String hostIPAddr = getHostIPAddressFromString(listenerIPAddress, niName);
			clientProps.put("listenerIPAddress", hostIPAddr);
		}
		
		// handle the list of appHandler information
		
		boolean continueProcessing = true;
		int number = 1;
		while (continueProcessing) {
			String respondToAddress;
			String respondToPort;	
			
			respondToAddress = clientProps.getProperty("respondToIPAddress."+number,"");
			respondToPort = clientProps.getProperty("respondToPort."+number, "80");
			
			if (clientProps.containsKey("respondToIPAddress."+number)) {
				String hostIPAddr = getHostIPAddressFromString(respondToAddress, niName);
				ProbeRespondToAddress rta = new ProbeRespondToAddress();
				rta.respondToAddress = hostIPAddr;
				rta.respondToPort = Integer.valueOf(respondToPort);
				respondToAddresses.add(rta);
			} else {
				continueProcessing = false;
			}
			number++;
			
		}		
		
		return clientProps;
	}
	
	private static void setDefaultProbeGeneratorProperties(Properties props) {
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
					while ((output = br.readLine()) != null) {
						buf.append(output);
					}
					
					response = buf.toString();
				}
			} finally {
				httpClient.close();
			}
			

		} catch (MalformedURLException e) {
			LOGGER.log(Level.SEVERE, "nThe respondTo URL was a no good.  respondTo URL is: "+url, e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "An IOException occured: the error message is - ", e);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Some error occured: the error message is - ", e);
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
