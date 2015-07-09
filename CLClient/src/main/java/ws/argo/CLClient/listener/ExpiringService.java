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

import java.util.Date;

import ws.argo.wireline.response.ServiceWrapper;

/**
 * The ExpiringService is a wrapper on a {@link ServiceWrapper} that will tell
 * the listener cache if its expired. Expiring services out of a cache is not an
 * Argo specific domain behavior and therefore that behavior is not in the Argo
 * domain classes.
 * 
 * @author jmsimpson
 *
 */
public class ExpiringService {

  static final long ONE_MINUTE_IN_MILLIS = 60000;

  ServiceWrapper service;
  public Date    cacheStartTime = new Date();

  public ExpiringService(ServiceWrapper service) {
    this.service = service;
  }

  /**
   * Determine if the service record is expired. The service record include a
   * TTL that tells the client how many minutes the client can count on the
   * information in the service record. If the cache times out, then those
   * service records that have expired should be expunged from the cache.
   * 
   * @return true if the service record should be expired from the cache
   */
  public boolean isExpired() {
    if (this.service.getTtl() == 0) {
      return false;
    }
    long t = this.cacheStartTime.getTime();
    Date validTime = new Date(t + (this.service.getTtl() * ONE_MINUTE_IN_MILLIS));
    Date now = new Date();

    return (now.getTime() > validTime.getTime());
  }

}
