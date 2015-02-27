package ws.argo.Responder;

import java.util.ArrayList;
import java.util.UUID;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ResponsePayloadBean {
	
	String probeID;
	String responseID;
	private ArrayList<ServiceInfoBean> responses = new ArrayList<ServiceInfoBean>();
	
	
	public ResponsePayloadBean(String probeID) {
		this.probeID = probeID;
		UUID uuid = UUID.randomUUID();
		this.responseID = "urn:uuid:"+uuid.toString();
	}
	


	public void addResponse(ServiceInfoBean entry) {
		responses.add(entry);
	}

	public String toXML() {
		StringBuffer buf = new StringBuffer();
		
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		buf.append("<responses responseID=\""+this.responseID+"\" probeID=\""+this.probeID+"\">\n");
		
		for (ServiceInfoBean infoBean : responses) {
			 buf.append(infoBean.toXML());
		}
		
		buf.append("</responses>\n");
		return buf.toString();
	}
	
	public String toJSON() {
		JSONObject response = this.toJSONObject();
		
		return response.toString(4);
	}
	
	public JSONObject toJSONObject() {
		JSONObject json = new JSONObject();
		JSONArray array = new JSONArray();
		
		for (ServiceInfoBean infoBean : responses) {
			 array.add(infoBean.toJSONObject());
		}	
		
		json.put("responseID", this.responseID);
		json.put("probeID", this.probeID);
		
		json.put("responses", array);
		
		return json;
		
	}
	
}
