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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

public class ResponseCache {

  private HashMap<String, ServiceInfoBean> cache = new HashMap<String, ServiceInfoBean>();

  public synchronized void cacheAll(ArrayList<ServiceInfoBean> list) {

    for (ServiceInfoBean service : list) {
      cache.put(service.getId(), service);
    }

  }

  public synchronized void cache(ServiceInfoBean service) {
    cache.put(service.getId(), service);
  }

  public String toJSON() {
    JSONObject response = this.toJSONObject();

    return response.toString(0);
  }

  public JSONObject toJSONObject() {
    JSONObject json = new JSONObject();
    JSONArray array = new JSONArray();

    clearExpired();
    for (ServiceInfoBean infoBean : cache.values()) {
      array.add(infoBean.toJSONObject());
    }

    json.put("cache", array);

    return json;

  }

  private synchronized void clearExpired() {

    Iterator<Entry<String, ServiceInfoBean>> it = cache.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, ServiceInfoBean> pair = it.next();
      ServiceInfoBean infoBean = (ServiceInfoBean) pair.getValue();
      if (infoBean.isExpired())
        it.remove(); // avoids a ConcurrentModificationException
    }

  }

  public String toContractJSON() {
    JSONObject json = new JSONObject();
    JSONArray array = new JSONArray();

    clearExpired();
    for (ServiceInfoBean infoBean : cache.values()) {
      JSONObject contract = new JSONObject();
      contract.put("contractID", infoBean.getServiceContractID());
      contract.put("contractDescription", infoBean.getContractDescription());
      array.add(contract);
    }

    json.put("contracts", array);

    return json.toString(4);
  }

}
