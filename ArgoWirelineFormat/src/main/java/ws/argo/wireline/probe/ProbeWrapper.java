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

  /*
   * This is a wrapper class for the label, URL pair for a respondTo address
   */
  public class RespondToURL {
    public String url;
    public String label;
  }

  private ArrayList<RespondToURL> respondToURLs      = new ArrayList<RespondToURL>();
  private ArrayList<String>       serviceContractIDs = new ArrayList<String>();
  private ArrayList<String>       serviceInstanceIDs = new ArrayList<String>();

  private String                  clientId;
  private String                  id;
  private String                  desVersion;
  private String                  respondToPayloadType;

  public ProbeWrapper(String probeID) {
    setProbeId(probeID);
    this.desVersion = PROBE_DES_VERSION;
  }

  /**
   * Determine whether this is a "select" all probe. It's called a "naked" probe
   * because the probe has no service contract ID or service ID qualifiers.
   * 
   * @return true if no service contract ID or service ID qualifiers.
   */
  public boolean isNaked() {
    boolean emptyScids = serviceContractIDs.isEmpty();
    boolean emptySiids = serviceInstanceIDs.isEmpty();

    return emptyScids && emptySiids;
  }

  // Getters and Setters

  public String getProbeId() {
    return id;
  }

  public void setProbeId(String id) {
    this.id = id;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getDESVersion() {
    return desVersion;
  }

  public void setDESVersion(String version) {
    this.desVersion = version;
  }

  public String getRespondToPayloadType() {
    return this.respondToPayloadType;
  }

  public void setRespondToPayloadType(String respondToPayloadType) {
    this.respondToPayloadType = respondToPayloadType;
  }

  public ArrayList<RespondToURL> getRespondToURLs() {
    return respondToURLs;
  }

  public List<String> getServiceContractIDs() {
    return serviceContractIDs;
  }

  public List<String> getServiceInstanceIDs() {
    return serviceInstanceIDs;
  }

  public void addRespondToURL(String label, String url) {

    RespondToURL respondToURL = new RespondToURL();
    respondToURL.label = label;
    respondToURL.url = url;

    respondToURLs.add(respondToURL);

  }

  /**
   * Add a service contact ID to include in the probe.
   * 
   * @param serviceContractID - the ID
   */
  public void addServiceContractID(String serviceContractID) {

    serviceContractIDs.add(serviceContractID);

  }

  /**
   * Add a service ID to include in the probe.
   * 
   * @param serviceInstanceID - the ID
   */
  public void addServiceInstanceID(String serviceInstanceID) {

    serviceInstanceIDs.add(serviceInstanceID);

  }

  /**
   * Returns the string form of the payload in XML.
   */
  public String asXML() {
    XMLSerializer serializer = new XMLSerializer();

    return serializer.marshal(this);

  }

}
