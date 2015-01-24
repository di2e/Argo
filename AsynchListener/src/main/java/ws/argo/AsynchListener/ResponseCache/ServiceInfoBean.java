package ws.argo.AsynchListener.ResponseCache;

import java.util.Date;

import net.sf.json.JSONObject;

public class ServiceInfoBean {

	public static final String HUMAN_CONSUMABLE = "HUMAN_CONSUMABLE";
	public static final String MACHINE_CONSUMABLE = "MACHINE_CONSUMABLE";
	static final long ONE_MINUTE_IN_MILLIS=60000;

	
	public String id;
	public String serviceContractID;
	public String ipAddress;
	public String port;
	public String url;
	public String data;
	public Integer ttl = 0; //0 means that is never expires
	public String description;
	public String contractDescription;
	public String serviceName;
	public String consumability = MACHINE_CONSUMABLE; // default to machine consumable

	public Date cacheStartTime = new Date();


	public ServiceInfoBean(String id) {
		this.id = id;
	}
	
	public String toXML() {
		StringBuffer buf = new StringBuffer();
		buf.append("\t<service id=\""+id+"\" contractID=\""+serviceContractID+"\">\n");
//		buf.append("\t\t<id>"+id+"</id>\n");		
//		buf.append("\t\t<serviceContractID>"+serviceContractID+"</serviceContractID>\n");
		if (ipAddress != null)
			buf.append("\t\t<ipAddress>"+ipAddress+"</ipAddress>\n");
		if (port != null)
			buf.append("\t\t<port>"+port+"</port>\n");
		if (url != null)
			buf.append("\t\t<url>"+url+"</url>\n");
		if (data != null)
			buf.append("\t\t<data>"+data+"</data>\n");
		if (ttl != null)
			buf.append("\t\t<ttl>"+ttl+"</ttl>\n");
		if (description != null)
			buf.append("\t\t<description>"+description+"</description>\n");
		if (contractDescription != null)
			buf.append("\t\t<contractDescription>"+contractDescription+"</contractDescription>\n");
		if (serviceName != null)
			buf.append("\t\t<serviceName>"+serviceName+"</serviceName>\n");
		if (consumability != null)
			buf.append("\t\t<consumability>"+consumability+"</consumability>\n");
		buf.append("\t</service>\n");
		
		return buf.toString();
	}
	
	public JSONObject toJSONObject() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("serviceContractID", serviceContractID);
		json.put("ipAddress", ipAddress);
		json.put("port", port);
		json.put("url", url);
		json.put("data", data);
		json.put("ttl", ttl);
		json.put("description", description);
		json.put("contractDescription", contractDescription);
		json.put("serviceName", serviceName);
		json.put("consumability", consumability);
	
		return json;		
	}
	
	public String toJSON() {
		return toJSONObject().toString(4);
	}
	
	public boolean isExpired() {	
		if (this.ttl == 0) return false;
		long t = this.cacheStartTime.getTime();
		Date validTime = new Date(t + (this.ttl * ONE_MINUTE_IN_MILLIS));		
		Date now = new Date();
		
		return (now.getTime() > validTime.getTime());
	}
}