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

package ws.argo.Responder.plugin.configFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import ws.argo.Responder.ProbePayloadBean;
import ws.argo.Responder.ResponsePayloadBean;
import ws.argo.Responder.ServiceInfoBean;
import ws.argo.Responder.plugin.configFile.xml.ServicesConfiguration;
import ws.argo.Responder.plugin.configFile.xml.ServicesConfiguration.Service;
import ws.argo.Responder.plugin.configFile.xml.ServicesConfiguration.Service.AccessPoints.AccessPoint;
import ws.argo.Responder.plugin.configFile.xml.ServicesConfiguration.Service.AccessPoints.AccessPoint.Data;


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
	

	public ResponsePayloadBean probeEvent(ProbePayloadBean payload) {
		return handleProbeEvent(payload);
	}
	
	public ResponsePayloadBean handleProbeEvent(ProbePayloadBean payload) {

		ResponsePayloadBean response = new ResponsePayloadBean(payload.probe.getId());
		
		LOGGER.fine("ConfigFileProbeHandlerPluginImpl handling probe: " + payload.toString());
		
		// do the actual lookup here
		//  and create and return the ResponderPayload
		// Can you say O(n^2) lookup?  Very bad - we can fix later
				
		if (payload.isNaked()) {
			LOGGER.fine("Query all detected - no service contract IDs in probe");
			for (ServiceInfoBean entry : serviceList) {			
				// If the set of contract IDs is empty, get all of them
				response.addResponse(entry);
			}
			
		} else {
			for (String serviceContractID : payload.probe.getScids().getServiceContractID()) {
				LOGGER.fine("Looking to detect "+serviceContractID+" in entry list.");
				for (ServiceInfoBean entry : serviceList) {			
					if (entry.getServiceContractID().equals(serviceContractID)) {
						// Boom Baby - we got one!!!
						response.addResponse(entry);
					}				
				}
			}
			for (String serviceInstanceID : payload.probe.getSiids().getServiceInstanceID()) {
				LOGGER.fine("Looking to detect "+serviceInstanceID+" in entry list.");
				for (ServiceInfoBean entry : serviceList) {			
					if (entry.getId().equals(serviceInstanceID)) {
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
		
		try {
			this.loadServiceConfigFile();
		} catch (JAXBException e) {
			LOGGER.log(Level.SEVERE, "Error loading configuation file: ", e);
		}
	}

	// This entire method is likely better done with JAXB.  This manual reading DOM process is
	// sooooooo prone to issues, it's not funny.  But, this is a prototype
	// TODO: make this method more solid
	private void loadServiceConfigFile() throws JAXBException, FileNotFoundException {
		
		
		JAXBContext jaxbContext = JAXBContext.newInstance(ServicesConfiguration.class);
		LOGGER.info("Loading configuration from "+this.configFilename);
		InputStream is = new FileInputStream(this.configFilename);
		
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		ServicesConfiguration services = (ServicesConfiguration) jaxbUnmarshaller.unmarshal(is);
				
		
		for (Service service : services.getService()) {
			
			ServiceInfoBean serviceBean = new ServiceInfoBean(service.getId());
			
			serviceBean.setServiceName(service.getServiceName());
			serviceBean.setDescription(service.getDescription());
			serviceBean.setContractDescription(service.getContractDescription());
			serviceBean.setConsumability(service.getConsumability());
			serviceBean.setServiceContractID(service.getContractID());
			serviceBean.setTtl(service.getTtl());
			
			List<AccessPoint> apList = service.getAccessPoints().getAccessPoint();
			
			for (AccessPoint ap : apList) {
				Data xmlData = ap.getData();
				serviceBean.addAccessPoint(ap.getLabel(), ap.getIpAddress(), ap.getPort(), ap.getUrl(), xmlData.getType(), xmlData.getValue());
			}
			
			serviceList.add(serviceBean);
		}


	}	

}
