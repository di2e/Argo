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

import ws.argo.wireline.probe.ProbeWrapper.RespondToURL;
import ws.argo.wireline.probe.xml.ObjectFactory;
import ws.argo.wireline.probe.xml.Probe;
import ws.argo.wireline.probe.xml.Probe.Ra.RespondTo;

/**
 * The XMLSerializer provides the translation between the ProbeWrapper and the
 * wireline strings. It can both marshall and unmarshall wireline protocol from
 * the domain objects.
 * 
 * @author jmsimpson
 *
 */
public class XMLSerializer {

  private ObjectFactory xmlProbeFactory = new ObjectFactory();

  public XMLSerializer() {
  }

  /**
   * Translate the ProbeWrapper object into the wireline string. See
   * {@link ProbeWrapper}.
   * 
   * @param probe the instance of the ProbeWrapper
   * @return the wireline string
   */
  public String marshal(ProbeWrapper probe) {

    Probe xmlProbe = this.composeProbeFromProbeWrapper(probe);

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

  private Probe composeProbeFromProbeWrapper(ProbeWrapper probe) {

    Probe xmlProbe = xmlProbeFactory.createProbe();

    xmlProbe.setClient(probe.getClientId());
    xmlProbe.setId(probe.getProbeId());
    xmlProbe.setDESVersion(probe.getDESVersion());
    xmlProbe.setRespondToPayloadType(probe.getRespondToPayloadType());

    for (RespondToURL url : probe.getRespondToURLs()) {
      RespondTo rt = xmlProbeFactory.createProbeRaRespondTo();
      rt.setLabel(url.label);
      rt.setValue(url.url);
      if (xmlProbe.getRa() == null)
        xmlProbe.setRa(xmlProbeFactory.createProbeRa());
      xmlProbe.getRa().getRespondTo().add(rt);
    }

    for (String scid : probe.getServiceContractIDs()) {
      if (xmlProbe.getScids() == null)
        xmlProbe.setScids(xmlProbeFactory.createProbeScids());
      xmlProbe.getScids().getServiceContractID().add(scid);
    }

    for (String siid : probe.getServiceInstanceIDs()) {
      if (xmlProbe.getSiids() == null)
        xmlProbe.setSiids(xmlProbeFactory.createProbeSiids());
      xmlProbe.getSiids().getServiceInstanceID().add(siid);
    }

    return xmlProbe;
  }

  /**
   * Create a new ProbeWrapper from the wireline payload.
   * 
   * @param payload - the string serialized probe that came directly off the
   *          wire. This should only be in XML
   * @return the ProbeWrapper instance
   * @throws ProbeParseException if there was an issue parsing the payload
   */
  public ProbeWrapper unmarshal(String payload) throws ProbeParseException {
    Probe xmlProbe = parseProbePayload(payload);

    ProbeWrapper probe = new ProbeWrapper(xmlProbe.getId());
    probe.setClientId(xmlProbe.getClient());
    probe.setDESVersion(xmlProbe.getDESVersion());
    probe.setRespondToPayloadType(xmlProbe.getRespondToPayloadType());

    if (xmlProbe.getRa() != null) {
      for (RespondTo respondToUrl : xmlProbe.getRa().getRespondTo()) {
        probe.addRespondToURL(respondToUrl.getLabel(), respondToUrl.getValue());
      }
    }

    if (xmlProbe.getScids() != null) {
      for (String scid : xmlProbe.getScids().getServiceContractID()) {
        probe.addServiceContractID(scid);
      }
    }

    if (xmlProbe.getSiids() != null) {
      for (String siid : xmlProbe.getSiids().getServiceInstanceID()) {
        probe.addServiceInstanceID(siid);
      }
    }

    return probe;
  }

  private static Probe parseProbePayload(String payload) throws ProbeParseException {

    JAXBContext jaxbContext;
    Probe probe = null;

    try {
      jaxbContext = JAXBContext.newInstance(Probe.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      InputStream inputStream = new ByteArrayInputStream(payload.getBytes(Charset.forName("UTF-8")));

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
