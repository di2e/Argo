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

package ws.argo.wireline.probe;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import ws.argo.wireline.probe.xml.Probe;
import ws.argo.wireline.probe.xml.ObjectFactory;
import ws.argo.wireline.probe.xml.Probe.Ra.RespondTo;

public class ProbeWrapper {

  public static final String PROBE_DES_VERSION = "urn:uuid:918b5b45-1496-459e-8a6b-633dbc465380";

  public static final String XML               = "XML";
  public static final String JSON              = "JSON";

  public Probe               xmlProbe;
  private ObjectFactory      xmlProbeFactory   = new ObjectFactory();

  public ProbeWrapper(String probeID) {
    xmlProbe = new ObjectFactory().createProbe();
    setProbeID(probeID);
  }

  private ProbeWrapper(Probe xmlProbe) {
    this.xmlProbe = xmlProbe;
  }

  /**
   * Determine whether this is a "select" all probe. It's called a "naked" probe
   * because the probe has no service contract ID or service ID qualifiers.
   * 
   * @return true if no service contract ID or service ID qualifiers.
   */
  public boolean isNaked() {
    boolean emptyScids = xmlProbe.getScids() == null || xmlProbe.getScids().getServiceContractID().isEmpty();
    boolean emptySiids = xmlProbe.getSiids() == null || xmlProbe.getSiids().getServiceInstanceID().isEmpty();

    return emptyScids && emptySiids;
  }

  // Getters and Setters

  public String getProbeID() {
    return xmlProbe.getId();
  }

  public void setProbeID(String id) {
    xmlProbe.setId(id);
  }

  public void setDESVersion(String version) {
    xmlProbe.setDESVersion(version);
  }

  public void setClientID(String clientID) {
    xmlProbe.setClient(clientID);
  }

  public void setRespondToPayloadType(String respondToPayloadType) {
    xmlProbe.setRespondToPayloadType(respondToPayloadType);
  }

  public void addRespondToURL(String label, String respondToURL) {

    RespondTo rt = xmlProbeFactory.createProbeRaRespondTo();
    rt.setLabel(label);
    rt.setValue(respondToURL);
    if (xmlProbe.getRa() == null)
      xmlProbe.setRa(xmlProbeFactory.createProbeRa());
    xmlProbe.getRa().getRespondTo().add(rt);

  }

  /**
   * Add a service contact ID to include in the probe.
   * 
   * @param serviceContractID - the ID
   */
  public void addServiceContractID(String serviceContractID) {
    if (xmlProbe.getScids() == null)
      xmlProbe.setScids(xmlProbeFactory.createProbeScids());
    xmlProbe.getScids().getServiceContractID().add(serviceContractID);
  }

  /**
   * Add a service ID to include in the probe.
   * 
   * @param serviceInstanceID - the ID
   */
  public void addServiceInstanceID(String serviceInstanceID) {
    if (xmlProbe.getSiids() == null)
      xmlProbe.setSiids(xmlProbeFactory.createProbeSiids());
    xmlProbe.getSiids().getServiceInstanceID().add(serviceInstanceID);
  }

  /**
   * Returns the string form of the payload in XML.
   */
  public String asXML() {
    StringWriter sw = new StringWriter();
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Probe.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

      // output pretty printed
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

      jaxbMarshaller.marshal(xmlProbe, sw);
    } catch (PropertyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JAXBException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return sw.toString();

  }

  public List<String> getServiceContractIDs() {
    return xmlProbe.getScids().getServiceContractID();
  }

  public List<String> getServiceInstanceIDs() {
    return xmlProbe.getSiids().getServiceInstanceID();
  }

  public ArrayList<String> getRespondToURLs() {

    ArrayList<String> respondToURLs = new ArrayList<String>();
    for (RespondTo respondTo : xmlProbe.getRa().getRespondTo()) {
      respondToURLs.add(respondTo.getValue());
    }

    return respondToURLs;
  }

  public String getRespondToPayloadType() {
    return xmlProbe.getRespondToPayloadType();
  }

  /**
   * Create a new ProbeWrapper from the wireline payload.
   * 
   * @param payload - the string serialized probe that came directly off the
   *          wire. This should only be in XML
   * @return the ProbeWrapper instance
   * @throws ProbeParseException if there was an issue parsing the payload
   */
  public static ProbeWrapper fromWireline(String payload) throws ProbeParseException {
    Probe xmlProbe = parseProbePayload(payload);
    ProbeWrapper wrapper = new ProbeWrapper(xmlProbe);
    return wrapper;
  }

  private static Probe parseProbePayload(String payload) throws ProbeParseException {

    JAXBContext jaxbContext;
    Probe probe = null;

    try {
      jaxbContext = JAXBContext.newInstance(Probe.class);
      // StringReader sr = new StringReader(payload);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      InputStream inputStream = new ByteArrayInputStream(payload.getBytes(Charset.forName("UTF-8")));

      // XMLStreamReader xmlStreamReader =
      // XMLInputFactory.newInstance().createXMLStreamReader(inputStream);

      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      SAXParser sp = spf.newSAXParser();
      XMLReader xmlReader = sp.getXMLReader();
      InputSource inputSource = new InputSource(inputStream);
      SAXSource saxSource = new SAXSource(xmlReader, inputSource);

      probe = (Probe) jaxbUnmarshaller.unmarshal(saxSource);

    } catch (JAXBException | FactoryConfigurationError | ParserConfigurationException | SAXException e) {
      throw new ProbeParseException(e);
    }

    return probe;
  }

}
