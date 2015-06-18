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

import java.util.HashSet;
import java.util.UUID;

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

  public boolean isEmpty() {
    return responses.isEmpty();
  }

  public int numberOfServices() {
    return responses.size();
  }

  public String getProbeID() {
    return probeID;
  }

  public String getResponseID() {
    return responseID;
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
