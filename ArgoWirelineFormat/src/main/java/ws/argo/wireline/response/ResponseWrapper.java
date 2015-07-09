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

package ws.argo.wireline.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * The ResponseWrapper is effectively an Argo domain class. It is the
 * intermediary data encapsulation object representation of a response. The
 * domain responses generally do not have any domain specific behavior so this
 * class is almost behavior-free.
 * 
 * @author jmsimpson
 *
 */
public class ResponseWrapper {
  String                          probeID;
  String                          responseID;
  private HashSet<ServiceWrapper> responses = new HashSet<ServiceWrapper>();

  /**
   * Create a new Response payload for a particular probe.
   * 
   * @param probeID - the ID of the probe as provided by the client
   */
  public ResponseWrapper(String probeID) {
    this.probeID = probeID;
    UUID uuid = UUID.randomUUID();
    this.responseID = "urn:uuid:" + uuid.toString();
  }

  @Override
  public int hashCode() {
    return responseID.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (!(obj instanceof ResponseWrapper))
      return false;

    ResponseWrapper resp = (ResponseWrapper) obj;

    if (!resp.getResponseID().trim().equals(this.getResponseID().trim()))
      return false;
    if (!resp.getProbeID().trim().equals(this.getProbeID().trim()))
      return false;

    if (resp.numberOfServices() != this.numberOfServices())
      return false;

    List<ServiceWrapper> svcs1 = new ArrayList<ServiceWrapper>(this.getServices());
    List<ServiceWrapper> svcs2 = new ArrayList<ServiceWrapper>(resp.getServices());

    if (svcs1.size() != svcs2.size())
      return false;

    Collections.sort(svcs1);
    Collections.sort(svcs2);

    for (int i = 0; i < svcs1.size(); i++) {
      if (!svcs1.get(i).equals(svcs2.get(i)))
        return false;
    }

    return true;

  }

  public boolean isEmpty() {
    return responses.isEmpty();
  }

  public int numberOfServices() {
    return responses.size();
  }

  public String getProbeID() {
    return probeID != null ? probeID : "";
  }

  public String getResponseID() {
    return responseID != null ? responseID : "";
  }

  public void setResponseID(String responseId) {
    this.responseID = responseId;
  }

  public void addResponse(ServiceWrapper entry) {
    responses.add(entry);
  }

  public HashSet<ServiceWrapper> getServices() {
    return responses;
  }

  /**
   * Return the XML string form of the Response payload.
   */
  public String toXML() {
    XMLSerializer serializer = new XMLSerializer();
    return serializer.marshal(this);

  }

  /**
   * Return the JSON string form of the Response payload.
   */
  public String toJSON() {
    JSONSerializer serializer = new JSONSerializer();
    return serializer.marshal(this);

  }
}
