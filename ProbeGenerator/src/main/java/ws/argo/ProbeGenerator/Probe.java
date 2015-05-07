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

package ws.argo.ProbeGenerator;

import java.util.ArrayList;
import java.util.UUID;

public class Probe {

	public static final String PROBE_GENERTOR_CONTRACT_ID= "urn:uuid:918b5b45-1496-459e-8a6b-633dbc465380";

	public static final String XML = "XML";
	public static final String JSON = "JSON";

	public String probeID;
	public String respondToPayloadType; // Should be XML or JSON
	public int ttl = 255; // the default TTL for a probe is the max TTL of 255 - or the entire network
	public ArrayList<String> serviceContractIDs = new ArrayList<String>();
	public ArrayList<String> respondToURLs = new ArrayList<String>();
	public ArrayList<String> serviceInstanceIDs = new ArrayList<String>();
	
	public Probe(String respondToPayloadType) {
		UUID uuid = UUID.randomUUID();
		probeID = "urn:uuid:"+uuid.toString();	
		this.respondToPayloadType = respondToPayloadType;
	}
	
	public void addRespondToURL(String respondToURL) {
		respondToURLs.add(respondToURL);
	}
	
	public void addServiceContractID(String serviceContractID) {
		serviceContractIDs.add(serviceContractID);
	}
	
	public void addServiceInstanceID(String serviceInstanceID) {
		serviceInstanceIDs.add(serviceInstanceID);
	}
	
	public String asXML() {
		StringBuffer buf = new StringBuffer();
		
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		buf.append("<probe id=\"")
			.append(probeID)
			.append("\" DESVersion=\"")
			.append(PROBE_GENERTOR_CONTRACT_ID)
			.append("\" >")
			.append("\n");

		buf.append("\t<respondToPayloadType>")
		.append(this.respondToPayloadType)
		.append("</respondToPayloadType>\n");
		
		//Add in the respondTo addresses
		buf.append("\t<ra>\n");
		for (String ra : serviceContractIDs ) {
			buf.append("\t\t<respondTo>").append(ra).append("</respondTo>\n");		
		}
		buf.append("\t</ra>\n");

		//Add in the service contract IDs
		buf.append("\t<scids>\n");
		for (String contractID : serviceContractIDs ) {
			buf.append("\t\t<serviceContractID>").append(contractID).append("</serviceContractID>\n");
		}
		buf.append("\t</scids>\n");
		
		//Add in the service instance IDs
		buf.append("\t<siids>\n");
		for (String instanceID : serviceInstanceIDs ) {
			buf.append("\t\t<serviceInstanceID>").append(instanceID).append("</serviceInstanceID>\n");
		}
		buf.append("\t</siids>\n");
		
		buf.append("</probe>\n\n");
		
		return buf.toString();
	}
	
}
