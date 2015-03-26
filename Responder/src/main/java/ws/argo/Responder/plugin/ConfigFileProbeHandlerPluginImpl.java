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

package ws.argo.Responder.plugin;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ws.argo.Responder.ProbePayloadBean;
import ws.argo.Responder.ResponsePayloadBean;
import ws.argo.Responder.ServiceInfoBean;


// This default probe handler will load up a list of IP addresses and port number associates
// with a service contract ID (like a UUID)

// This handler will read a config xml file which lists the services that it can respond with
public class ConfigFileProbeHandlerPluginImpl implements ProbeHandlerPluginIntf {
	
	private final static Logger LOGGER = Logger.getLogger(ConfigFileProbeHandlerPluginImpl.class.getName());

	private static final String SERVICE = "service";

	Properties config = new Properties();
	
	// This really needs to be better than an O(n^2) lookup - like an O(n log n) with a HashMap with a list value.  But I'm lazy at the moment
	ArrayList<ServiceInfoBean> serviceList = new ArrayList<ServiceInfoBean>();
	
	private String configFilename;
	private boolean debug = false;
	

	public ResponsePayloadBean probeEvent(ProbePayloadBean payload) {
		return handleProbeEvent(payload);
	}
	
	public ResponsePayloadBean handleProbeEvent(ProbePayloadBean payload) {

		ResponsePayloadBean response = new ResponsePayloadBean(payload.probeID);
		
		LOGGER.fine("ConfigFileProbeHandlerPluginImpl handling probe: " + payload.toString());
		
		// do the actual lookup here
		//  and create and return the ResponderPayload
		// Can you say O(n^2) lookup?  Very bad - we can fix later
				
		
		if (payload.serviceContractIDs.isEmpty()) {
			LOGGER.fine("Query all detected - no service contract IDs in probe");
			for (ServiceInfoBean entry : serviceList) {			
				// If the set of contract IDs is empty, get all of them
				response.addResponse(entry);
			}
			
		} else {
			for (String serviceContractID : payload.serviceContractIDs) {
				LOGGER.fine("Looking to detect "+serviceContractID+" in entry list.");
				for (ServiceInfoBean entry : serviceList) {			
					if (entry.serviceContractID.equals(serviceContractID)) {
						// Boom Baby - we got one!!!
						response.addResponse(entry);
					}				
				}
			}
		}
		
		return response;	
		
	}

	public void setPropertiesFilename(String filename) throws IOException {
		 
		config.load(new FileInputStream(filename));
		
		this.configFilename = config.getProperty("xmlConfigFilename");
		this.debug = Boolean.parseBoolean(config.getProperty("debug", "false"));
		
		try {
			this.loadServiceConfigFile();
		} catch (SAXException e) {
			LOGGER.severe("Error loading configuation file: "+e.getMessage());
		}
	}

	// This entire method is likely better done with JAXB.  This manual reading DOM process is
	// sooooooo prone to issues, it's not funny.  But, this is a prototype
	// TODO: make this method more solid
	private void loadServiceConfigFile() throws SAXException, IOException {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
				.newInstance();
		builderFactory.setCoalescing(true);
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LOGGER.severe("XML Parser error: "+e.getMessage());
		}		
		
		LOGGER.info("Loading configuration from "+this.configFilename);
		InputStream is = new FileInputStream(this.configFilename);
		Document document = builder.parse(is);

		NodeList list = document.getElementsByTagName(SERVICE);

		for (int i = 0; i < list.getLength(); i++) {
			Element service = (Element) list.item(i);
			String contractID = null;
			String serviceID = null;

			contractID = service.getAttribute("contractID");
			serviceID = service.getAttribute("id");

			ServiceInfoBean config = new ServiceInfoBean(serviceID);
			
			config.serviceContractID = contractID;
			
			// Need some better error handling here.  The config file MUST have all 4 config items in it 
			// or bad things happen.
			Node n;
			n = service.getElementsByTagName("ipAddress").item(0);
			config.ipAddress = ((Element) n).getTextContent();
			n = service.getElementsByTagName("port").item(0);
			config.port = ((Element) n).getTextContent();
			n = service.getElementsByTagName("url").item(0);
			config.url = ((Element) n).getTextContent();
			n = service.getElementsByTagName("data").item(0);
			config.data = ((Element) n).getTextContent();
			n = service.getElementsByTagName("description").item(0);
			config.description = ((Element) n).getTextContent();
			n = service.getElementsByTagName("contractDescription").item(0);
			config.contractDescription = ((Element) n).getTextContent();
			n = service.getElementsByTagName("serviceName").item(0);
			config.serviceName = ((Element) n).getTextContent();
			n = service.getElementsByTagName("consumability").item(0);
			config.consumability = ((Element) n).getTextContent();
			
			n = service.getElementsByTagName("ttl").item(0);
			try {
				config.ttl = Integer.decode(((Element) n).getTextContent());
			} catch (NumberFormatException e) {
				LOGGER.warning("Attempting to read the TTL from serviceName \""+config.serviceName+"\" "+e.getMessage());
			}
			
			serviceList.add(config);
			
		}

	}	

}
