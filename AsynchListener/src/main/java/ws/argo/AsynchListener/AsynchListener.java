package ws.argo.AsynchListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ws.argo.AsynchListener.ResponseCache.ResponseCache;
import ws.argo.AsynchListener.ResponseCache.ServiceInfoBean;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


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
		
		ArrayList<ServiceInfoBean> serviceList = parseProbeResponseXML(probeResponseXML);
		
		cache.cacheAll(serviceList);
		
		return "Asynch Listener Cached "+serviceList.size()+" services from probe response\n";
	}
	
	private ArrayList<ServiceInfoBean> parseProbeResponseJSON(String jsonString) throws SAXException, IOException {
		ArrayList<ServiceInfoBean> serviceList = new ArrayList<ServiceInfoBean>();
		
		JSONObject repsonseJSON = JSONObject.fromObject(jsonString);
		
		JSONArray responses = (JSONArray) repsonseJSON.get("responses");
		
		Iterator<Object> it = responses.iterator();
		
		while (it.hasNext()) {
			JSONObject serviceInfo = (JSONObject) it.next();
			String serviceID = serviceInfo.optString("id");
			
			ServiceInfoBean config = new ServiceInfoBean(serviceID);
			
			config.serviceContractID = serviceInfo.optString("serviceContractID");
			config.ipAddress = serviceInfo.optString("ipAddress");
			config.port = serviceInfo.optString("port");
			config.url = serviceInfo.optString("url");
			config.data = serviceInfo.optString("data");
			config.description = serviceInfo.optString("description");
			config.contractDescription = serviceInfo.optString("contractDescription");
			config.serviceName = serviceInfo.optString("serviceName");
			config.ttl = serviceInfo.optInt("ttl", 0);
			config.consumability = serviceInfo.optString("consumability");
			
			serviceList.add(config);
		}
		
		
		return serviceList;
	}
	
	
	// This entire method is likely better done with JAXB.  This manual reading DOM process is
	// sooooooo prone to issues, it's not funny.  But, this is a prototype
	// TODO: make this method more solid
	private ArrayList<ServiceInfoBean> parseProbeResponseXML(String xmlString) throws SAXException, IOException {
		ArrayList<ServiceInfoBean> serviceList = new ArrayList<ServiceInfoBean>();
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory .newInstance();
		builderFactory.setCoalescing(true);
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}		
		
		InputStream is = IOUtils.toInputStream(xmlString);
		Document document = builder.parse(is);

		NodeList list = document.getElementsByTagName("service");

		for (int i = 0; i < list.getLength(); i++) {
			Element service = (Element) list.item(i);
			String contractID = null;
			String serviceID = null;

			contractID = service.getAttribute("contractID");
			serviceID = service.getAttribute("id");

			ServiceInfoBean config = new ServiceInfoBean(serviceID);
			
			config.serviceContractID = contractID;
			
			// Need some better error handling here.  The xml MUST have all config items in it 
			// or bad things happen.
			Node n;
			n = service.getElementsByTagName("ipAddress").item(0);
			config.ipAddress = ((Element) n).getTextContent();
			n = service.getElementsByTagName("port").item(0);
			config.port = ((Element) n).getTextContent();
			n = service.getElementsByTagName("url").item(0);
			config.url = ((Element) n).getTextContent();
			n = service.getElementsByTagName("data").item(0);
			config.data = ((Element) n).getTextContent();
			n = service.getElementsByTagName("description").item(0);
			config.description = ((Element) n).getTextContent();
			n = service.getElementsByTagName("contractDescription").item(0);
			config.contractDescription = ((Element) n).getTextContent();
			n = service.getElementsByTagName("serviceName").item(0);
			config.serviceName = ((Element) n).getTextContent();
			n = service.getElementsByTagName("consumability").item(0);
			config.consumability = ((Element) n).getTextContent();
			n = service.getElementsByTagName("ttl").item(0);
			config.ttl = Integer.decode(((Element) n).getTextContent());
			
			serviceList.add(config);
			
		}
		return serviceList;

	}	

	
}
