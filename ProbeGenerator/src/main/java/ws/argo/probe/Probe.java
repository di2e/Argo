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

package ws.argo.probe;

import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.validator.routines.UrlValidator;

import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.probe.ProbeWrapper.RespondToURL;

/**
 * This class represents a Probe that will be sent from a client. It's a wrapper
 * on ProbeWrapper class (which is a little weird but work with me).
 * 
 * <p>The client should not know anything about any other intermediary classes.
 * 
 * @author jmsimpson
 *
 */
public class Probe {

  public static final String JSON = ProbeWrapper.JSON;
  public static final String XML  = ProbeWrapper.XML;

  // the default TTL for a probe is the max TTL of 255 - or the entire network
  int ttl = 255;

  private ProbeWrapper _probe;

  /**
   * Create a new probe from the argument that includes all the frame stuff
   * (payload type, respondTo addr, etc) but don't include any of the contract
   * or service ids. This method is a helper method for splitting over sized
   * probes into smaller probes so that the packets are not too big for the
   * network.
   * 
   * @param p the source probe
   * @return the probe frame
   * @throws ProbeSenderException if something went wrong
   */
  public static Probe frameProbeFrom(Probe p) throws ProbeSenderException {
    Probe fp = null;
    try {
      fp = new Probe(p.getProbeWrapper().getRespondToPayloadType());
    } catch (UnsupportedPayloadType e1) {
      throw new ProbeSenderException("Error in creating frame probe.", e1);
    }

    fp.setHopLimit(p.getHopLimit());
    fp.setClientID(p.getClientID());
    for (RespondToURL respondTo : p.getProbeWrapper().getRespondToURLs()) {
      try {
        fp.addRespondToURL(respondTo.getLabel(), respondTo.getUrl());
      } catch (MalformedURLException e) {
        throw new ProbeSenderException("Error in creating frame probe.", e);
      }
    }
    return fp;

  }

  /**
   * Create a new client-generated probe for sending out on the network.
   * 
   * @param respondToPayloadType - JSON or XML
   * @throws UnsupportedPayloadType if the respondToPayloadType is not JSON or
   *           XML
   */
  public Probe(String respondToPayloadType) throws UnsupportedPayloadType {

    String probeID = createProbeID();

    _probe = new ProbeWrapper(probeID);

    _probe.setDESVersion(ProbeWrapper.PROBE_DES_VERSION);

    setRespondToPayloadType(respondToPayloadType);
  }

  /**
   * Copy constructor.
   * 
   * @param probe the probe to copy
   * @throws UnsupportedPayloadType if the payload in the original somehow
   *           magically morphed into a type that Probes no longer supported
   * @throws MalformedURLException if the url magically becomes malformed
   */
  public Probe(Probe probe) throws UnsupportedPayloadType, MalformedURLException {
    this(probe.getProbeWrapper().getRespondToPayloadType());

    this.setHopLimit(probe.getHopLimit());
    this.setClientID(probe.getClientID());
    for (RespondToURL respondTo : probe.getProbeWrapper().getRespondToURLs()) {
      this.addRespondToURL(respondTo.getLabel(), respondTo.getUrl());
    }
    for (String scid : probe.getProbeWrapper().getServiceContractIDs()) {
      this.addServiceContractID(scid);
    }
    for (String siid : probe.getProbeWrapper().getServiceInstanceIDs()) {
      this.addServiceInstanceID(siid);
    }
  }

  protected ProbeWrapper getProbeWrapper() {
    return _probe;
  }

  private String createProbeID() {
    UUID uuid = UUID.randomUUID();
    String probeID = "urn:uuid:" + uuid.toString();
    return probeID;
  }

  /**
   * get the hop limit or TTL for the probe.
   * @return the hop limit
   */
  public int getHopLimit() {
    return ttl;
  }

  /**
   * set the hop limit or TTL for the probe.
   * 
   * @param limit an integer from 0 to 255
   */
  public void setHopLimit(int limit) {
    this.ttl = limit;
  }

  /**
   * get the probe ID.
   * 
   * @return the probe ID
   */
  public String getProbeID() {
    return _probe.getProbeId();
  }

  public String getClientID() {
    return _probe.getClientId();
  }

  /**
   * set the client ID. The client ID can be used be a responder for whatever
   * reason it deems necessary.
   * 
   * @param clientID the client identifier
   */
  public void setClientID(String clientID) {
    _probe.setClientId(clientID);
  }

  /**
   * set the payload type for the response from the Responder. It only support
   * XML and JSON.
   * 
   * @param respondToPayloadType a payload type string - XML or JSON
   * @throws UnsupportedPayloadType if you don't pass in XML or JSON
   */
  public void setRespondToPayloadType(String respondToPayloadType) throws UnsupportedPayloadType {
    // Sanity check on the payload type values. Should be XML or JSON
    // If the probe goes out with a bad value here, then the Responder may have
    // problems
    if (respondToPayloadType == null || respondToPayloadType.isEmpty() || (!respondToPayloadType.equals(ProbeWrapper.JSON) && !respondToPayloadType.equals(ProbeWrapper.XML)))
      throw new UnsupportedPayloadType("Attempting to set payload type to: " + respondToPayloadType + ". Cannot be null or empty and must be " + ProbeWrapper.JSON + " or " + ProbeWrapper.XML);

    _probe.setRespondToPayloadType(respondToPayloadType);
  }

  /**
   * Add a URL that specifies where the Responder should send the response.
   * Provide a label as a hint to the Responder about why this URL was included.
   * For example: give a "internal" label for a respondToURL that is only
   * accessible from inside a NATed network.
   * 
   * @param label - hint about the URL
   * @param respondToURL - the URL the responder will POST a payload to
   * @throws MalformedURLException if the URL is bad
   */
  public void addRespondToURL(String label, String respondToURL) throws MalformedURLException {

    // Sanity check on the respondToURL
    // The requirement for the respondToURL is a REST POST call, so that means
    // only HTTP and HTTPS schemes.
    // Localhost is allowed as well as a valid response destination
    String[] schemes = { "http", "https" };
    UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
    if (!urlValidator.isValid(respondToURL))
      throw new MalformedURLException("The probe respondTo URL is invalid: " + respondToURL);

    _probe.addRespondToURL(label, respondToURL);

  }

  /**
   * Add a service contact ID to include in the probe.
   * 
   * @param serviceContractID - the ID
   */
  public void addServiceContractID(String serviceContractID) {
    _probe.addServiceContractID(serviceContractID);
  }

  /**
   * Add a service ID to include in the probe.
   * 
   * @param serviceInstanceID - the ID
   */
  public void addServiceInstanceID(String serviceInstanceID) {
    _probe.addServiceInstanceID(serviceInstanceID);
  }

  /**
   * Return the XML string of the probe. Probes are only serialized as XML.
   * 
   * @return the XML representation
   * @throws JAXBException if there is some issue building XML
   */
  public String asXML() throws JAXBException {
    return _probe.asXML();
  }

  public String asXMLFragment() throws JAXBException {
    return _probe.asXMLFragment();
  }

  /**
   * This will return the single-line textual representation. This was crafted
   * for the command line client. Your mileage may vary.
   * 
   * @return the single line description
   */
  public String asString() {
    return _probe.asString();
  }

  /**
   * Create and return a LinkedList of a combination of both scids and siids.
   * This is a method that is used primarily in the creation of split packets.
   * 
   * @return combined LikedList
   */
  public LinkedList<ProbeIdEntry> getCombinedIdentifierList() {
    LinkedList<ProbeIdEntry> combinedList = new LinkedList<ProbeIdEntry>();

    for (String id : this.getProbeWrapper().getServiceContractIDs()) {
      combinedList.add(new ProbeIdEntry("scid", id));
    }
    for (String id : this.getProbeWrapper().getServiceInstanceIDs()) {
      combinedList.add(new ProbeIdEntry("siid", id));
    }
    return combinedList;
  }

  /**
   * This is a "Pair" class that in context specific in the pairing of the
   * context (scid | siid) and the actual id. This needs to be done do package
   * and differentiate IDs that are piled into the same list.
   * 
   * @author jmsimpson
   *
   */
  public static class ProbeIdEntry {
    String type;
    String id;

    public ProbeIdEntry(String type, String id) {
      this.type = type;
      this.id = id;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

  }

}
