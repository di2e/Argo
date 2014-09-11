package net.di2e.rtsd.Responder;

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
			buf.append(serviceContractIDs+"\n");
		}
			
		buf.append("****** Probe Payload End ******\n");
		return buf.toString();
	}
}