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

	public static final String PROBE_GENERTOR_CONTRACT_ID= "urn:uuid:55f1fecc-bfed-4be0-926b-b36a138a9943";

	public static final String XML = "XML";
	public static final String JSON = "JSON";

	public String probeID;
	public String respondToURL;
	public String respondToPayloadType; // Should be XML or JSON
	public int ttl = 255; // the default TTL for a probe is the max TTL of 255 - or the entire network
	public ArrayList<String> serviceContractIDs = new ArrayList<String>();
	
	public Probe(String respondToURL, String respondToPayloadType) {
		UUID uuid = UUID.randomUUID();
		probeID = "urn:uuid:"+uuid.toString();	
		this.respondToURL = respondToURL;
		this.respondToPayloadType = respondToPayloadType;
	}
	
	public void addServiceContractID(String serviceContractID) {
		serviceContractIDs.add(serviceContractID);
	}
	
	public String asXML() {
		StringBuffer buf = new StringBuffer();
		
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		buf.append("<probe id=\"")
			.append(probeID)
			.append("\" contractID=\"")
			.append(PROBE_GENERTOR_CONTRACT_ID)
			.append("\" >")
			.append("\n");
		
		buf.append("\t<respondTo>")
			.append(this.respondToURL)
			.append("</respondTo>\n");

		buf.append("\t<respondToPayloadType>")
			.append(this.respondToPayloadType)
			.append("</respondToPayloadType>\n");
		
		for (String contractID : serviceContractIDs ) {
			buf.append("\t<serviceContractID>")
				.append(contractID)
				.append("</serviceContractID>\n");
		}
		
		
		buf.append("</probe>\n\n");
		
		return buf.toString();
	}
	
}
