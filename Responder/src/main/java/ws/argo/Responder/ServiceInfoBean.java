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

import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;

import ws.argo.ArgoWirelineFormat.response.xml.ObjectFactory;
import ws.argo.ArgoWirelineFormat.response.xml.Services.Service;
import ws.argo.ArgoWirelineFormat.response.xml.Services.Service.AccessPoints;
import ws.argo.ArgoWirelineFormat.response.xml.Services.Service.AccessPoints.AccessPoint;
import ws.argo.ArgoWirelineFormat.response.xml.Services.Service.AccessPoints.AccessPoint.Data;

public class ServiceInfoBean {

	public static final String HUMAN_CONSUMABLE = "HUMAN_CONSUMABLE";
	public static final String MACHINE_CONSUMABLE = "MACHINE_CONSUMABLE";

	public Service xmlService;

	public ServiceInfoBean(String id) {
		ObjectFactory of = new ObjectFactory();
		xmlService = of.createServicesService();
		this.setId(id);
	}
	
	public int hashCode() {
		return this.getId().hashCode();
	}

	public Service getXmlService() {
		return xmlService;
	}

	public List<AccessPoint> getAccessPoints() {
		return xmlService.getAccessPoints().getAccessPoint();
	}

	public void addAccessPoint(String label, String ip, String port,
			String url, String dataType, String data) {

		ObjectFactory of = new ObjectFactory();
		AccessPoint ap = of.createServicesServiceAccessPointsAccessPoint();
		Data xmlData = of.createServicesServiceAccessPointsAccessPointData();
		ap.setLabel(label);
		ap.setIpAddress(ip);
		ap.setPort(port);
		ap.setUrl(url);
		xmlData.setType(dataType);
		xmlData.setValue(data);
		ap.setData(xmlData);

		AccessPoints aps = xmlService.getAccessPoints();
		if (aps == null) {
			aps = of.createServicesServiceAccessPoints();
			xmlService.setAccessPoints(aps);
		}

		xmlService.getAccessPoints().getAccessPoint().add(ap);

	}

	public String getId() {
		return xmlService.getId();
	}

	public void setId(String id) {
		xmlService.setId(id);
	}

	public String getServiceContractID() {
		return xmlService.getContractID();
	}

	public void setServiceContractID(String serviceContractID) {
		xmlService.setContractID(serviceContractID);
	}

	public String getServiceName() {
		return xmlService.getServiceName();
	}

	public void setServiceName(String serviceName) {
		xmlService.setServiceName(serviceName);
	}

	public String getDescription() {
		return xmlService.getDescription();
	}

	public void setDescription(String description) {
		xmlService.setDescription(description);
	}

	public String getContractDescription() {
		return xmlService.getContractDescription();
	}

	public void setContractDescription(String contractDescription) {
		xmlService.setContractDescription(contractDescription);
	}

	public String getConsumability() {
		return xmlService.getConsumability();
	}

	public void setConsumability(String consumability) {
		xmlService.setConsumability(consumability);
	}

	public Integer getTtl() {
		Integer ttl = 0;
		try {
			ttl = Integer.valueOf(xmlService.getTtl());
		} catch (NumberFormatException e) {
			// TODO: Put LOGGER heer
		}
		return ttl;
	}

	public void setTtl(Integer ttl) {
		xmlService.setTtl(Integer.toString(ttl.intValue()));
	}

	public void setTtl(String ttlString) {
		xmlService.setTtl(ttlString);
	}

	public String toXML() {
		StringWriter sw = new StringWriter();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Service.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(xmlService, sw);
		} catch (PropertyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sw.toString();
	}

	public JSONObject toJSONObject() {
		JSONObject json = new JSONObject();

		json.put("id", this.getId());
		json.put("serviceContractID", this.getServiceContractID());
		json.put("serviceName", this.getServiceName());
		json.put("description", this.getDescription());
		json.put("contractDescription", this.getContractDescription());
		json.put("consumability", this.getConsumability());
		json.put("ttl", this.getTtl().toString());

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