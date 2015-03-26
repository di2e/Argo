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
package ws.argo.Responder.plugin.jmdns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

import ws.argo.Responder.ProbePayloadBean;
import ws.argo.Responder.ResponsePayloadBean;
import ws.argo.Responder.ServiceInfoBean;
import ws.argo.Responder.plugin.ConfigFileProbeHandlerPluginImpl;
import ws.argo.Responder.plugin.ProbeHandlerPluginIntf;

public class MulticastDNSProbeHandlerPluginImpl implements ServiceListener, ServiceTypeListener,
		ProbeHandlerPluginIntf {

	private final static Logger LOGGER = Logger.getLogger(ConfigFileProbeHandlerPluginImpl.class.getName());

	protected JmDNS jmmDNS;
	ArrayList<ServiceInfoBean> serviceList = new ArrayList<ServiceInfoBean>();
	
	public MulticastDNSProbeHandlerPluginImpl() {
		try {
			initialize();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public ResponsePayloadBean probeEvent(ProbePayloadBean payload) {
		// TODO Auto-generated method stub
		ResponsePayloadBean response = new ResponsePayloadBean(payload.probeID);
		
		LOGGER.fine("Handling probe: " + payload.toString());
		
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
		
		return response;		}

	@Override
	public void setPropertiesFilename(String filename) throws IOException {
		// TODO Auto-generated method stub
		LOGGER.info("Does not support loading props");
	}
	
	private void initialize() throws IOException {
		this.jmmDNS = JmDNS.create();  //JmmDNS.Factory.getInstance();
        try {
			this.jmmDNS.addServiceTypeListener(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        LOGGER.setLevel(Level.INFO);
	}


	@Override
	public void serviceTypeAdded(ServiceEvent event) {
		// TODO Auto-generated method stub
		LOGGER.info("mDNS Service Type Added: "+ event.getType());
		this.jmmDNS.addServiceListener(event.getType(), this);
		
	}


	@Override
	public void subTypeForServiceTypeAdded(ServiceEvent event) {
		// TODO Auto-generated method stub
		LOGGER.info("mDNS Service Sub-Type Added: "+ event.getType());
		
	}


	@Override
	public void serviceAdded(ServiceEvent event) {
		// TODO Auto-generated method stub
		LOGGER.info("mDNS Service Added: "+ event.getName());
		
	}


	@Override
	public void serviceRemoved(ServiceEvent event) {
		// TODO Auto-generated method stub
		LOGGER.info("mDNS Service Removed: "+ event.getName());
		
		
	}


	@Override
	public void serviceResolved(ServiceEvent event) {
		LOGGER.info("mDNS Service RESOLVED: "+ event.toString());
		
		if (!event.getInfo().hasData()) {
			LOGGER.warning("mDNS Service has no data - skipping: "+ event.toString());
			return;
		}
		
		String contractID = null;
		String serviceID = null;

		contractID = event.getInfo().getType();
		serviceID = event.getInfo().getQualifiedName();
		
		ServiceInfoBean config = new ServiceInfoBean(event.getInfo().getKey());
		
		config.serviceContractID = contractID;
		config.serviceName = event.getInfo().getName();

		java.net.Inet4Address[] ipv4Addresses = event.getInfo().getInet4Addresses();
		config.ipAddress = ipv4Addresses[0].getHostAddress();
		
		config.port = Integer.toString(event.getInfo().getPort());
		
		java.lang.String[] urls = event.getInfo().getURLs();
		StringBuffer buf = new StringBuffer();
		for (String url : event.getInfo().getURLs()) {
			buf.append(url+" ");
		}
		//config.url = buf.toString();
		config.url = urls[0];
		
		config.description = event.getInfo().getQualifiedName();
		
		config.data = event.getInfo().getNiceTextString();
		
//		String tempData = new String(config.data);
//		
//		List<String> allMatches = new ArrayList<String>();
//		Matcher m = Pattern.compile("(\\\\[0-9]{3})").matcher(tempData);
//		while (m.find()) {
//			allMatches.add(m.group());
//		}
//		for (String octal: allMatches) {
//			int charNum = Integer.parseInt(octal.substring(1));
//			String replacementChar = Character.toString ((char) charNum);
//			tempData = tempData.replace(octal, replacementChar);
//		}
//		
//		config.data = tempData;
	
		config.contractDescription = event.getInfo().getApplication();
		
		config.consumability = ServiceInfoBean.MACHINE_CONSUMABLE;
		
		config.ttl = 0;
		
		
		serviceList.add(config);		
		
	}

}
