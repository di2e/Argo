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

import java.util.HashSet;
import java.util.UUID;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ResponsePayloadBean {
	
	String probeID;
	String responseID;
	private HashSet<ServiceInfoBean> responses = new HashSet<ServiceInfoBean>();
	
	
	public ResponsePayloadBean(String probeID) {
		this.probeID = probeID;
		UUID uuid = UUID.randomUUID();
		this.responseID = "urn:uuid:"+uuid.toString();
	}
	
	public boolean isEmpty() {
		return responses.isEmpty();
	}
	
	public int numberOfServices() {
		return responses.size();
	}

	public String getProbeID() {
		return probeID;
	}

	public String getResponseID() {
		return responseID;
	}

	public void addResponse(ServiceInfoBean entry) {
		responses.add(entry);
	}

	public String toXML() {
		StringBuffer buf = new StringBuffer();
		
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		buf.append("<services responseID=\""+this.responseID+"\" probeID=\""+this.probeID+"\">\n");
		
		for (ServiceInfoBean infoBean : responses) {
			 buf.append(infoBean.toXML());
		}
		
		buf.append("</services>\n");
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
		
		json.put("services", array);
		
		return json;
		
	}
	
}
