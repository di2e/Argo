package ws.argo.CLClient.listener;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.xml.sax.SAXException;

import ws.argo.ProbeGenerator.xml.Probe;
import ws.argo.Responder.response.xml.Services;
import ws.argo.Responder.response.xml.Services.Service;
import ws.argo.Responder.response.xml.Services.Service.AccessPoints.AccessPoint;
import ws.argo.Responder.response.xml.Services.Service.AccessPoints.AccessPoint.Data;


@Path("/listener")
public class ProbeResponseResource {

	private static ResponseCache cache = new ResponseCache();
	

	@POST
	@Path("/probeResponse")
	@Consumes("application/json")
	public String handleJSONProbeResponse(String probeResponseJSON) throws SAXException, IOException {
		System.out.println("Listener receiving JSON probe response: "+probeResponseJSON);
		
		ArrayList<ServiceInfoBean> serviceList = parseProbeResponseJSON(probeResponseJSON);
		
		cache.cacheAll(serviceList);
		
		return probeResponseJSON;
	}
	
	@POST
	@Path("/probeResponse")
	@Consumes("application/xml")
	public String handleXMLProbeResponse(String probeResponseXML) throws SAXException, IOException, JAXBException {
		System.out.println("Listener receiving XML probe response: "+probeResponseXML);
		
		ArrayList<ServiceInfoBean> serviceList = parseProbeResponseXML(probeResponseXML);
		
		cache.cacheAll(serviceList);
		
		return probeResponseXML;
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

	private ArrayList<ServiceInfoBean> parseProbeResponseXML(String probeResponseXML) throws JAXBException {
		ArrayList<ServiceInfoBean> serviceList = new ArrayList<ServiceInfoBean>();
		JAXBContext jaxbContext = JAXBContext.newInstance(Services.class);
		 
		StringReader sr = new StringReader(probeResponseXML);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Services xmlServices = (Services) jaxbUnmarshaller.unmarshal(sr);
				
		List<Service> xmlServiceList = xmlServices.getService();
		
		for (Service xmlService : xmlServiceList) {
			JSONObject serviceInfo = convertToJSON(xmlService);
			
			ServiceInfoBean config = new ServiceInfoBean(serviceInfo);
			
			serviceList.add(config);			
		}
		
		return serviceList;
	}

	private JSONObject convertToJSON(Service xmlService) {
		JSONObject json = new JSONObject();
		
		json.put("id", xmlService.getId());
		json.put("serviceContractID", xmlService.getContractID());
		json.put("serviceName", xmlService.getServiceName());
		json.put("description", xmlService.getDescription());
		json.put("contractDescription", xmlService.getContractDescription());
		json.put("consumability", xmlService.getConsumability());
		json.put("ttl", xmlService.getTtl().toString());

		JSONArray array = new JSONArray();

		for (AccessPoint ap : xmlService.getAccessPoints().getAccessPoint()) {
			JSONObject jsonAP = new JSONObject();

			jsonAP.put("label", ap.getLabel());
			jsonAP.put("ipAddress", ap.getIpAddress());
			jsonAP.put("port", ap.getPort());
			jsonAP.put("url", ap.getUrl());

			if (ap.getData() != null) {
				Data xmlData = ap.getData();
				jsonAP.put("dataType", xmlData.getType());
				byte[] bytesEncoded = Base64.encodeBase64(xmlData.getValue()
						.getBytes());
				String encodedString = new String(bytesEncoded);
				jsonAP.put("data", encodedString);
			} else {
				jsonAP.put("dataType", "");
				jsonAP.put("data", "");
			}

			array.add(jsonAP);
		}

		json.put("accessPoints", array);
		
		return json;
	}
	
}
