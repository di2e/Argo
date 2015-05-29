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

package ws.argo.responder.test.listener;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ServiceInfoBean {

  static final long ONE_MINUTE_IN_MILLIS = 60000;

  JSONObject        jsonService;
  String            data;

  List<AccessPoint> accessPoints         = null;

  public Date       cacheStartTime       = new Date();

  public class AccessPoint {
    JSONObject jsonAP;

    public AccessPoint(JSONObject jsonAP) {
      this.jsonAP = jsonAP;
    }

    public String getLabel() {
      return this.jsonAP.optString("label", "");
    }

    public String getIPAddress() {
      return this.jsonAP.optString("ipAddress", "");
    }

    public String getPort() {
      return this.jsonAP.optString("port", "");
    }

    public String getURL() {
      return this.jsonAP.optString("url", "");
    }

    public String getData() {
      if (data == null) {
        String base64encoded = jsonAP.optString("data", "");
        byte[] bytes = Base64.decodeBase64(base64encoded);
        data = new String(bytes);
      }

      return data;
    }

  }

  public ServiceInfoBean(JSONObject jsonService) {
    this.jsonService = jsonService;
  }

  public int hashCode() {
    return this.getId().hashCode();
  }

  public String getId() {
    return jsonService.getString("id");
  }

  public String getServiceName() {
    return jsonService.getString("serviceName");
  }

  public String getDescription() {
    return jsonService.getString("description");
  }

  public String getServiceContractID() {
    return jsonService.getString("serviceContractID");
  }

  public String getContractDescription() {
    return jsonService.getString("contractDescription");
  }

  public String getConsumability() {
    return jsonService.getString("consumability");
  }

  public Integer getTtl() {
    String ttlString = jsonService.optString("ttl", "0");
    Integer ttl = 0;
    try {
      ttl = Integer.parseInt(ttlString);
    } catch (NumberFormatException e) {}

    return ttl;
  }

  @SuppressWarnings("unchecked")
  public List<AccessPoint> getAccessPoints() {
    if (accessPoints == null) {
      JSONArray aps = jsonService.getJSONArray("accessPoints");

      if (!aps.isEmpty()) {

        accessPoints = new ArrayList<AccessPoint>();
        Iterator<Object> apIterator = aps.iterator();

        while (apIterator.hasNext()) {
          JSONObject apInfo = (JSONObject) apIterator.next();

          AccessPoint ap = new AccessPoint(apInfo);

          accessPoints.add(ap);
        }
      }
    }
    return accessPoints;
  }

  public boolean isExpired() {
    if (this.getTtl() == 0)
      return false;
    long t = this.cacheStartTime.getTime();
    Date validTime = new Date(t + (this.getTtl() * ONE_MINUTE_IN_MILLIS));
    Date now = new Date();

    return (now.getTime() > validTime.getTime());
  }

  public Object toJSONObject() {
    return jsonService;
  }

}