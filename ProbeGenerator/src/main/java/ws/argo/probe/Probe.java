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
import java.util.ArrayList;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.validator.routines.UrlValidator;

import ws.argo.wireline.probe.ProbeWrapper;

public class Probe {

  // the default TTL for a probe is the max TTL of 255 - or the entire network
  public int ttl = 255;

  public ArrayList<String> serviceInstanceIDs = new ArrayList<String>();

  private ProbeWrapper probe;

  /**
   * Create a new client-generated probe for sending out on the network.
   * 
   * @param respondToPayloadType - JSON or XML
   * @throws UnsupportedPayloadType if the respondToPayloadType is not JSON or
   *           XML
   */
  public Probe(String respondToPayloadType) throws UnsupportedPayloadType {

    UUID uuid = UUID.randomUUID();
    String probeID = "urn:uuid:" + uuid.toString();

    probe = new ProbeWrapper(probeID);

    probe.setDESVersion(ProbeWrapper.PROBE_DES_VERSION);

    setRespondToPayloadType(respondToPayloadType);
  }

  public int getHopLimit() {
    return ttl;
  }

  public void setHopLimit(int limit) {
    this.ttl = limit;
  }

  public String getProbeID() {
    return probe.getProbeId();
  }

  public void setClientID(String clientID) {
    probe.setClientId(clientID);
  }

  public void setRespondToPayloadType(String respondToPayloadType) throws UnsupportedPayloadType {
    // Sanity check on the payload type values. Should be XML or JSON
    // If the probe goes out with a bad value here, then the Responder may have
    // problems
    if (respondToPayloadType == null || respondToPayloadType.isEmpty() || (!respondToPayloadType.equals(ProbeWrapper.JSON) && !respondToPayloadType.equals(ProbeWrapper.XML)))
      throw new UnsupportedPayloadType("Attempting to set payload type to: " + respondToPayloadType + ". Cannot be null or empty and must be " + ProbeWrapper.JSON + " or " + ProbeWrapper.XML);

    probe.setRespondToPayloadType(respondToPayloadType);
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

    probe.addRespondToURL(label, respondToURL);

  }

  /**
   * Add a service contact ID to include in the probe.
   * 
   * @param serviceContractID - the ID
   */
  public void addServiceContractID(String serviceContractID) {
    probe.addServiceContractID(serviceContractID);
  }

  /**
   * Add a service ID to include in the probe.
   * 
   * @param serviceInstanceID - the ID
   */
  public void addServiceInstanceID(String serviceInstanceID) {
    probe.addServiceInstanceID(serviceInstanceID);
  }

  /**
   * Return the XML string of the probe. Probes are only serialized as XML.
   * 
   * @throws JAXBException if there is some issue building XML
   */
  public String asXML() throws JAXBException {

    return probe.asXML();
  }

}
