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

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import ws.argo.ArgoWirelineFormat.probe.xml.Probe;


public class ProbePayloadBean {
	public Probe probe;

	public ProbePayloadBean(String payload) throws JAXBException {
		probe = this.parseProbePayload(payload);
	}
	
	public boolean isNaked() {
		boolean emptyScids = probe.getScids() == null || probe.getScids().getServiceContractID().isEmpty();
		boolean emptySiids = probe.getSiids() == null || probe.getSiids().getServiceInstanceID().isEmpty();

		return emptyScids && emptySiids;
	}
	

	private Probe parseProbePayload(String payload) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(Probe.class);
		 
		StringReader sr = new StringReader(payload);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Probe probe = (Probe) jaxbUnmarshaller.unmarshal(sr);
				
		return probe;
	}
	
	public String toString() {	
		StringWriter sw = new StringWriter();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Probe.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
 
			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
 
			jaxbMarshaller.marshal(probe, sw);
		} catch (PropertyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sw.toString();	

	}
}