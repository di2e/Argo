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

package ws.argo.CLClient.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import ws.argo.wireline.response.ServiceWrapper;

import com.google.gson.Gson;

/**
 * The ResponseCache is a utility class for an Argo client that will maintain a
 * list of responses that come back from probes. It works in conjunction with
 * the ExpiringService class with is a wrapper on the wireline domain classes
 * that keep track of their creation timestamp. This cache will timeout any
 * ExpiringService instances that live longer than the cache timeout.
 * 
 * @author jmsimpson
 *
 */
public class ResponseCache {

  private HashMap<String, ExpiringService> cache = new HashMap<String, ExpiringService>();

  private class Cache {
    public ArrayList<ServiceWrapper> cache = new ArrayList<ServiceWrapper>();
  }

  /**
   * Put a list of services in the cache.
   * 
   * @param list the list of service to put in the cache
   */
  public synchronized void cacheAll(ArrayList<ExpiringService> list) {

    for (ExpiringService service : list) {
      cache.put(service.service.getId(), service);
    }

  }

  public synchronized void cache(ExpiringService service) {
    cache.put(service.service.getId(), service);
  }

  /**
   * Return the JSON string of the response cache. Leverage the Gson nature of
   * the domain wireline classes
   * 
   * @return the JSON string
   */
  public String asJSON() {
    Gson gson = new Gson();

    clearExpired();

    Cache jsonCache = new Cache();
    for (ExpiringService svc : cache.values()) {
      jsonCache.cache.add(svc.service);
    }

    String json = gson.toJson(jsonCache);

    return json;
  }

  private synchronized void clearExpired() {

    Iterator<Entry<String, ExpiringService>> it = cache.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, ExpiringService> pair = it.next();
      ExpiringService infoBean = (ExpiringService) pair.getValue();
      if (infoBean.isExpired()) {
        it.remove(); // avoids a ConcurrentModificationException
      }
    }

  }

}
