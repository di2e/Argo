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

package ws.argo.ProbeGenerator;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.validator.routines.UrlValidator;

import ws.argo.ArgoWirelineFormat.probe.xml.ObjectFactory;
import ws.argo.ArgoWirelineFormat.probe.xml.Probe.Ra.RespondTo;

public class Probe {

	public static final String    PROBE_GENERTOR_CONTRACT_ID	= "urn:uuid:918b5b45-1496-459e-8a6b-633dbc465380";

	public static final String    XML	                    = "XML";
	public static final String    JSON	                    = "JSON";

	//  the default TTL for a probe is the max TTL of 255 - or the entire network
	public int                    ttl	                    = 255;

	public ArrayList<String>      serviceInstanceIDs	    = new ArrayList<String>();

	private ws.argo.ArgoWirelineFormat.probe.xml.Probe      xmlProbe;
	private ObjectFactory         xmlProbeFactory	        = new ObjectFactory();

	public Probe(String respondToPayloadType) throws UnsupportedPayloadType {
		xmlProbe = xmlProbeFactory.createProbe();

		UUID uuid = UUID.randomUUID();
		String probeID = "urn:uuid:" + uuid.toString();
		xmlProbe.setId(probeID);
		xmlProbe.setDESVersion(PROBE_GENERTOR_CONTRACT_ID);
		if (respondToPayloadType == null
		        || respondToPayloadType.isEmpty()
		        || (!respondToPayloadType.equals(JSON) && !respondToPayloadType
		                .equals(XML)))
			throw new UnsupportedPayloadType(
			        "Attempting to set payload type to: " + respondToPayloadType
			                + ". Cannot be null or empty and must be " + JSON + " or " + XML);
		xmlProbe.setRespondToPayloadType(respondToPayloadType); // Should be XML
																// or JSON
	}

	public int getHopLimit() {
		return ttl;
	}

	public void setHopLimit(int limit) {
		this.ttl = limit;
	}

	public String getProbeID() {
		return xmlProbe.getId();
	}

	public void setClientID(String clientID) {
		xmlProbe.setClient(clientID);
	}

	public void addRespondToURL(String label, String respondToURL) throws MalformedURLException {

		String[] schemes = { "http", "https" };
		UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
		if (!urlValidator.isValid(respondToURL)) {
			throw new MalformedURLException("The probe respondTo URL is invalid: " + respondToURL);
		}
		RespondTo rt = xmlProbeFactory.createProbeRaRespondTo();
		rt.setLabel(label);
		rt.setValue(respondToURL);
		if (xmlProbe.getRa() == null)
			xmlProbe.setRa(xmlProbeFactory.createProbeRa());
		xmlProbe.getRa().getRespondTo().add(rt);
	}

	public void addServiceContractID(String serviceContractID) {
		if (xmlProbe.getScids() == null)
			xmlProbe.setScids(xmlProbeFactory.createProbeScids());
		xmlProbe.getScids().getServiceContractID().add(serviceContractID);
	}

	public void addServiceInstanceID(String serviceInstanceID) {
		if (xmlProbe.getSiids() == null)
			xmlProbe.setSiids(xmlProbeFactory.createProbeSiids());
		xmlProbe.getSiids().getServiceInstanceID().add(serviceInstanceID);
	}

	public String asXML() throws JAXBException {

		StringWriter sw = new StringWriter();
		JAXBContext jaxbContext = JAXBContext.newInstance(ws.argo.ArgoWirelineFormat.probe.xml.Probe.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// output pretty printed
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		jaxbMarshaller.marshal(xmlProbe, sw);

		return sw.toString();
	}

}
