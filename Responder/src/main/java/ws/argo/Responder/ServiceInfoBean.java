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

import net.sf.json.JSONObject;

public class ServiceInfoBean {
	
	public static final String HUMAN_CONSUMABLE = "HUMAN_CONSUMABLE";
	public static final String MACHINE_CONSUMABLE = "MACHINE_CONSUMABLE";
	
	public String id;
	public String serviceContractID;
	public String ipAddress;
	public String port;
	public String url;
	public String data;
	public Integer ttl = 0;
	public String description;
	public String contractDescription;
	public String serviceName;
	public String consumability = MACHINE_CONSUMABLE; // default to machine consumable


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
		json.put("contractDescription", contractDescription);
		json.put("serviceName", serviceName);
		json.put("description", description);
		json.put("consumability", consumability);
	
		return json;		
	}
}