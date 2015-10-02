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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The ProbeWrapper is effectively an Argo domain class. It is the intermediary
 * data encapsulation object representation of a probe. The domain probes
 * generally do not have any domain specific behavior so this class is
 * behavior-free.
 * 
 * @author jmsimpson
 *
 */
public class ProbeWrapper {

  public static final String PROBE_DES_VERSION = "urn:uuid:918b5b45-1496-459e-8a6b-633dbc465380";

  public static final String XML  = "XML";
  public static final String JSON = "JSON";

  /**
   * This is a wrapper class for the label, URL pair for a respondTo address.
   */
  public static class RespondToURL implements Comparable<RespondToURL> {
    public String url;
    public String label;

    public String getUrl() {
      return url != null ? url : "";
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getLabel() {
      return label != null ? label : "";
    }

    public void setLabel(String label) {
      this.label = label;
    }

    @Override
    public int hashCode() {
      return (getUrl() + getLabel()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this)
        return true;
      if (!(obj instanceof RespondToURL))
        return false;

      RespondToURL rturl = (RespondToURL) obj;

      String rturl1 = rturl.getUrl().trim() + rturl.getLabel().trim();
      String rturl2 = this.getUrl().trim() + this.getLabel().trim();

      if (!rturl1.equals(rturl2))
        return false;

      return true;
    }

    @Override
    public int compareTo(RespondToURL o) {
      String rturl1 = o.getUrl().trim() + o.getLabel().trim();
      String rturl2 = this.getUrl().trim() + this.getLabel().trim();

      return rturl1.compareTo(rturl2);
    }

  }

  private ArrayList<RespondToURL> respondToURLs      = new ArrayList<RespondToURL>();
  private ArrayList<String>       serviceContractIDs = new ArrayList<String>();
  private ArrayList<String>       serviceInstanceIDs = new ArrayList<String>();

  private String clientId;
  private String id;
  private String desVersion;
  private String respondToPayloadType;

  public ProbeWrapper(String probeID) {
    setProbeId(probeID);
    this.desVersion = PROBE_DES_VERSION;
  }

  @Override
  public int hashCode() {
    return this.getProbeId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (!(obj instanceof ProbeWrapper))
      return false;

    ProbeWrapper probe = (ProbeWrapper) obj;

    if (!probe.getProbeId().trim().equals(this.getProbeId().trim()))
      return false;
    if (!probe.getClientId().trim().equals(this.getClientId().trim()))
      return false;
    if (!probe.getDESVersion().trim().equals(this.getDESVersion().trim()))
      return false;
    if (!probe.getRespondToPayloadType().trim().equals(this.getRespondToPayloadType().trim()))
      return false;

    List<RespondToURL> rturls1 = new ArrayList<RespondToURL>(this.getRespondToURLs());
    List<RespondToURL> rturls2 = new ArrayList<RespondToURL>(probe.getRespondToURLs());

    if (rturls1.size() != rturls2.size())
      return false;

    Collections.sort(rturls1);
    Collections.sort(rturls2);

    for (int i = 0; i < rturls1.size(); i++) {
      if (!rturls1.get(i).equals(rturls2.get(i)))
        return false;
    }

    StringBuffer buf = new StringBuffer();

    for (String scid : this.getServiceContractIDs()) {
      buf.append(scid);
    }

    String scids1;
    scids1 = buf.toString();

    buf = new StringBuffer();

    for (String scid : probe.getServiceContractIDs()) {
      buf.append(scid);
    }

    String scids2;
    scids2 = buf.toString();

    if (!scids1.equals(scids2))
      return false;

    buf = new StringBuffer();

    for (String siid : this.getServiceInstanceIDs()) {
      buf.append(siid);
    }

    String siids1;
    siids1 = buf.toString();

    buf = new StringBuffer();

    for (String siid : probe.getServiceInstanceIDs()) {
      buf.append(siid);
    }

    String siids2;
    siids2 = buf.toString();

    if (!siids1.equals(siids2))
      return false;

    return true;
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
    return id != null ? id : "";
  }

  public void setProbeId(String id) {
    this.id = id;
  }

  public String getClientId() {
    // clientID can be null
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getDESVersion() {
    return desVersion != null ? desVersion : "";
  }

  public void setDESVersion(String version) {
    this.desVersion = version;
  }

  public String getRespondToPayloadType() {
    return respondToPayloadType != null ? respondToPayloadType : "";
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

  /**
   * Add a response URL to the probe.
   * 
   * @param label the string label for the respondTo address
   * @param url the actual URL string
   */
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
   * 
   * @return the XML payload
   */
  public String asXML() {
    XMLSerializer serializer = new XMLSerializer();

    return serializer.marshal(this);

  }
  
  /**
   * Returns the string form of the payload as an XML fragment.
   * 
   * @return the XML payload
   */
  public String asXMLFragment() {
    XMLSerializer serializer = new XMLSerializer();

    return serializer.marshalFragment(this);

  }
  
  /**
   * This will return the single-line textual representation. This was crafted
   * for the command line client. Your mileage may vary.
   * 
   * @return the single line description
   */
  public String asString() {
    StringBuffer buf = new StringBuffer();
    buf.append(" PID: [ ").append(this.id).append(" ]");;
    buf.append(" CID: [ ").append(this.clientId).append(" ]");;
    buf.append(" DES: [ ").append(this.desVersion).append(" ]");;
    buf.append(" SCIDS: [ " );
    for (String scid : serviceContractIDs) {
      buf.append(scid).append(" ");
    }
    buf.append("]");
    buf.append(" SIIDS: [ " );
    for (String siid : serviceInstanceIDs) {
      buf.append(siid).append(" ");
    }
    buf.append("]");
    buf.append(" RESPOND_TO: [ " );
    for (RespondToURL rt : respondToURLs) {
      buf.append(rt.label).append(", ").append(rt.url).append(" ");
    }
    buf.append("]");
    buf.append(" PAYLOAD_TYPE: [ ").append(respondToPayloadType).append(" ]");
    
    return buf.toString();
  }

}
