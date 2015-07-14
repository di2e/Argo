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
import java.util.List;
import java.util.logging.Logger;

/**
 * The ServiceWrapper is effectively an Argo domain class. It is the
 * intermediary data encapsulation object representation of a service (which is
 * usually wrapped up in a response). The domain services generally do not have
 * any domain specific behavior so this class is almost behavior-free.
 * 
 * @author jmsimpson
 *
 */
public class ServiceWrapper implements Comparable<ServiceWrapper> {

  private static final Logger LOGGER = Logger.getLogger(ServiceWrapper.class.getName());

  public static final String HUMAN_CONSUMABLE   = "HUMAN_CONSUMABLE";
  public static final String MACHINE_CONSUMABLE = "MACHINE_CONSUMABLE";

  // public Service xmlService;

  public String                 id;
  public String                 serviceContractId;
  public String                 serviceName;
  public String                 description;
  public String                 contractDescription;
  public String                 consumability;
  public Integer                ttl;
  public ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();

  /**
   * This is a convenience class for encapsulating access points.
   * 
   * @author jmsimpson
   *
   */
  public static class AccessPoint implements Comparable<AccessPoint> {
    public String label;
    public String url;
    public String ipAddress;
    public String port;
    public String dataType;
    public String data;

    @Override
    public int hashCode() {
      StringBuffer buf = new StringBuffer();

      buf.append(getLabel().trim());
      buf.append(getUrl().trim());
      buf.append(getIpAddress().trim());
      buf.append(getPort().trim());
      buf.append(getDataType().trim());
      buf.append(getData().trim());
      return buf.toString().hashCode();
    }

    @Override
    public int compareTo(AccessPoint ap) {
      if (ap == this)
        return 0;
      if (ap.hashCode() > this.hashCode()) {
        return 1;
      } else {
        return -1;
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null)
        return false;
      if (obj == this)
        return true;
      return this.hashCode() == obj.hashCode();
    }

    public String getLabel() {
      return label != null ? label : "";
    }

    public String getUrl() {
      return url != null ? url : "";
    }

    public String getIpAddress() {
      return ipAddress != null ? ipAddress : "";
    }

    public String getPort() {
      return port != null ? port : "";
    }

    public String getDataType() {
      return dataType != null ? dataType : "";
    }

    public String getData() {
      return data != null ? data : "";
    }

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

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (!(obj instanceof ServiceWrapper))
      return false;

    ServiceWrapper svc = (ServiceWrapper) obj;

    if (!svc.getId().trim().equals(this.getId().trim()))
      return false;
    if (!svc.getServiceName().trim().equals(this.getServiceName().trim()))
      return false;
    if (!svc.getServiceContractID().trim().equals(this.getServiceContractID().trim()))
      return false;
    if (!svc.getDescription().trim().equals(this.getDescription().trim()))
      return false;
    if (!svc.getContractDescription().trim().equals(this.getContractDescription().trim()))
      return false;
    if (!svc.getConsumability().trim().equals(this.getConsumability().trim()))
      return false;
    if (!svc.getTtl().equals(this.getTtl()))
      return false;

    List<AccessPoint> apl1 = new ArrayList<AccessPoint>(this.getAccessPoints());
    List<AccessPoint> apl2 = new ArrayList<AccessPoint>(svc.getAccessPoints());

    if (apl1.size() != apl2.size())
      return false;

    Collections.sort(apl1);
    Collections.sort(apl2);

    for (int i = 0; i < apl1.size(); i++) {
      if (!apl1.get(i).equals(apl2.get(i)))
        return false;
    }

    return true;
  }

  @Override
  public int compareTo(ServiceWrapper o) {
    if (o == this)
      return 0;
    if (o.hashCode() > this.hashCode()) {
      return 1;
    } else {
      return -1;
    }
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
  public void addAccessPoint(String label, String ip, String port, String url, String dataType, String data) {

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
    return id != null ? id : "";
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getServiceContractID() {
    return serviceContractId != null ? serviceContractId : "";
  }

  public void setServiceContractID(String serviceContractID) {
    this.serviceContractId = serviceContractID;
  }

  public String getServiceName() {
    return serviceName != null ? serviceName : "";
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getDescription() {
    return description != null ? description : "";
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getContractDescription() {
    return contractDescription != null ? contractDescription : "";
  }

  public void setContractDescription(String contractDescription) {
    this.contractDescription = contractDescription;
  }

  public String getConsumability() {
    return consumability != null ? consumability : "";
  }

  public void setConsumability(String consumability) {
    this.consumability = consumability;
  }

  /**
   * Return the Time To Live (network hops).
   */
  public Integer getTtl() {
    if (ttl == null)
      return 0;
    return ttl;
  }

  public void setTtl(Integer ttl) {
    this.ttl = ttl;
  }

  /**
   * Set the Time To Live (TTL) for the services. This method takes a string and
   * is provided as a convenience for developers (like me) that just wnat to jam
   * the string into the wrapper and let it worry about converting it.
   * 
   * @param ttlString
   */
  public void setTtl(String ttlString) {
    try {
      this.ttl = Integer.valueOf(ttlString);
    } catch (NumberFormatException e) {
      LOGGER.warning("Error trying to format the string " + ttlString + " into an Integer.");
    }
  }

}
