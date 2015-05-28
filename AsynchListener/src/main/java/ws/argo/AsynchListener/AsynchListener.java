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

package ws.argo.AsynchListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.xml.sax.SAXException;

import ws.argo.AsynchListener.ResponseCache.ResponseCache;
import ws.argo.AsynchListener.ResponseCache.ServiceInfoBean;


@Path("responseHandler")
public class AsynchListener {
	
	private static ResponseCache cache = new ResponseCache();
	

	@GET
	@Path("/testService")
	public String testService() {
		return "this is a test service for the AsynchListener.  yea.";
	}

	@GET
	@Path("/responses")
	@Produces("application/json")
	public String getResponses() {
		return cache.toJSON();
	}
	
	@GET
	@Path("/clearCache")
	@Produces("application/json")
	public String clearCache() {
		cache = new ResponseCache();
		return "Cleared Cache";
	}
	
	@GET
	@Path("/contracts")
	@Produces("application/json")
	public String getContracts() {
		return cache.toContractJSON();
	
	}
	
	@POST
	@Path("/probeResponse")
	@Consumes("application/json")
	public String handleJSONProbeResponse(String probeResponseJSON) throws SAXException, IOException {
		System.out.println("handling JSON probe response: "+probeResponseJSON);
		
		ArrayList<ServiceInfoBean> serviceList = parseProbeResponseJSON(probeResponseJSON);
		
		cache.cacheAll(serviceList);
		
		
		return "Asynch Listener Cached "+serviceList.size()+" services from probe response\n";
	}
	
	
	@POST
	@Path("/probeResponse")
	@Consumes("application/xml")
	public String handleXMLProbeResponse(String probeResponseXML) throws SAXException, IOException {
		System.out.println("handling XML probe response: "+probeResponseXML);
		
		// TODO: Yea, make this work.
//		ArrayList<ServiceInfoBean> serviceList = parseProbeResponseXML(probeResponseXML);
//		
//		cache.cacheAll(serviceList);
		
		return "Asynch Listener currently does not handle XML probe responses\n";
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<ServiceInfoBean> parseProbeResponseJSON(String jsonString) throws SAXException, IOException {
		ArrayList<ServiceInfoBean> serviceList = new ArrayList<ServiceInfoBean>();
		
		JSONObject repsonseJSON = JSONObject.fromObject(jsonString);
		
		JSONArray responses = (JSONArray) repsonseJSON.get("services");
		
		Iterator<Object> it = responses.iterator();
		
		while (it.hasNext()) {
			JSONObject serviceInfo = (JSONObject) it.next();
			
			ServiceInfoBean config = new ServiceInfoBean(serviceInfo);
			
			serviceList.add(config);
		}
		
		
		return serviceList;
	}
	

	
}
