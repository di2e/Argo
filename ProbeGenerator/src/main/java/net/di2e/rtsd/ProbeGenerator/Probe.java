package net.di2e.rtsd.ProbeGenerator;

import java.util.ArrayList;
import java.util.UUID;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

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
	
	// Do we need to cook this up as JSON?
	// The multicast responder doens't care and XML is easy enough to gin up as text from anywhere
	public JSONObject asJSONObject() {
		JSONObject json = new JSONObject();
		
		json.put("probeID", probeID);
		json.put("contractID", PROBE_GENERTOR_CONTRACT_ID);
		json.put("respondTo", respondToURL);
		json.put("respondToPayloadType", respondToPayloadType);
		
		JSONArray contractIDs = new JSONArray();
		
		for (String contractID : serviceContractIDs ) {
			contractIDs.add(contractID);
		}
		
		json.put("serviceContractIDs", contractIDs);
		
		return json;
	}
	
	public String asJSON() {
		JSONObject json = asJSONObject();
		return json.toString(4);
	}
	
}
