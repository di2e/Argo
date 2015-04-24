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

package ws.argo.Responder;

import java.util.ArrayList;

public class ProbePayloadBean {
	public String contractID;
	public String probeID;
	public String respondToURL;
	public String respondToPayloadType;
	public ArrayList<String> serviceContractIDs;

	public ProbePayloadBean() {
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		
		buf.append("****** Probe Payload Start ******\n")
			.append("\tID: ").append(probeID).append("\n")
			.append("\tcontractID: ").append(contractID).append("\n")
			.append("\trespondTo URL: ").append(respondToURL).append("\n")
			.append("\trespondTo payload type: ").append(respondToPayloadType).append("\n");
		
		buf.append("\tService Contract IDs: \n");

		for (String serviceContractID : serviceContractIDs) {
			buf.append(serviceContractID+"\n");
		}
			
		buf.append("****** Probe Payload End ******\n");
		return buf.toString();
	}
}