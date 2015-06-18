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
import java.util.List;
import java.util.logging.Logger;

public class ServiceWrapper {

  private static final Logger   LOGGER             = Logger.getLogger(ServiceWrapper.class.getName());

  public static final String    HUMAN_CONSUMABLE   = "HUMAN_CONSUMABLE";
  public static final String    MACHINE_CONSUMABLE = "MACHINE_CONSUMABLE";

  // public Service xmlService;

  public String                 id;
  public String                 serviceContractId;
  public String                 serviceName;
  public String                 description;
  public String                 contractDescription;
  public String                 consumability;
  public Integer                ttl;
  public ArrayList<AccessPoint> accessPoints       = new ArrayList<AccessPoint>();

  public class AccessPoint {
    public String label;
    public String url;
    public String ipAddress;
    public String port;
    public String dataType;
    public String data;
  }

  /**
   * Create a new Service Record.
   * 
   * @param id - the ID of the service record - should be globally unique for a
   *          particular service
   */
  public ServiceWrapper(String id) {
    this.setId(id);
  }

  public int hashCode() {
    return this.getId().hashCode();
  }

  public List<AccessPoint> getAccessPoints() {
    return accessPoints;
  }

  /**
   * Add a new access point for the service record. This structure exists to
   * help deal with network access issues and not multiple contracts. For
   * example, a service could have an access point that is different because of
   * multiple NICs that host is accessible on. It's not to have a service
   * accessible by HTTP and HTTPS - those are actually two different service
   * contract IDs. So if you have multiple access points that are actually
   * providing connection Information for different contracts, then you've done
   * something wrong.
   * 
   * <p>
   * Understand that you don't need to fill out all the fields in the service
   * record. Just the ones you need to provide to the client to satisfy the
   * service contract. For example, a REST services likely only needs the URL
   * provided and a database might need the IP address, port and URL.
   * 
   * <p>
   * Or, it could be a total free-for-all and you can put in totally obscure and
   * whacky connection information into the data field.
   * 
   * @param label - hint about why you might need this access point e.g.
   *          internal or external network
   * @param ip - Plain IP address
   * @param port - port the service is listening on
   * @param url - the URL (or portion of the URL) that the client needs to
   *          connect
   * @param dataType - a hint about what might be in the data section
   * @param data - totally free-form data (it's BASE64 encoded in both the XML
   *          and JSON)
   */
  public void addAccessPoint(String label, String ip, String port,
      String url, String dataType, String data) {

    AccessPoint ap = new AccessPoint();
    ap.label = label;
    ap.ipAddress = ip;
    ap.port = port;
    ap.url = url;
    ap.dataType = dataType;
    ap.data = data;

    accessPoints.add(ap);

  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getServiceContractID() {
    return serviceContractId;
  }

  public void setServiceContractID(String serviceContractID) {
    this.serviceContractId = serviceContractID;
  }

  public String getServiceName() {
    return this.serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getContractDescription() {
    return this.contractDescription;
  }

  public void setContractDescription(String contractDescription) {
    this.contractDescription = contractDescription;
  }

  public String getConsumability() {
    return this.consumability;
  }

  public void setConsumability(String consumability) {
    this.consumability = consumability;
  }

  /**
   * Return the Time To Live (network hops).
   */
  public Integer getTtl() {
    return ttl;
  }

  public void setTtl(Integer ttl) {
    this.ttl = ttl;
  }

  public void setTtl(String ttlString) {
    try {
      this.ttl = Integer.valueOf(ttlString);
    } catch (NumberFormatException e) {
      LOGGER.warning("Error trying to format the string " + ttlString + " into an Integer.");
    }
  }

}
