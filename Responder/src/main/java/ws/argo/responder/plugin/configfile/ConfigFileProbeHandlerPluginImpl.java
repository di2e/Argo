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

package ws.argo.responder.plugin.configfile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;
import java.util.logging.Logger;

import ws.argo.responder.ProbeHandlerPluginIntf;
import ws.argo.responder.ProbePayloadBean;
import ws.argo.responder.ResponsePayloadBean;
import ws.argo.responder.ServiceInfoBean;

// This default probe handler will load up a list of IP addresses and port number associates
// with a service contract ID (like a UUID)

// This handler will read a config xml file which lists the services that it can respond with
public class ConfigFileProbeHandlerPluginImpl implements ProbeHandlerPluginIntf {

  private static final Logger LOGGER      = Logger.getLogger(ConfigFileProbeHandlerPluginImpl.class.getName());

  Properties                  config      = new Properties();

  // This really needs to be better than an O(n^2) lookup - like an O(n log n)
  // with a HashMap with a list value. But I'm lazy at the moment
  ArrayList<ServiceInfoBean>  serviceList = new ArrayList<ServiceInfoBean>();

  private Timer               configFileScan;

  public ResponsePayloadBean probeEvent(ProbePayloadBean payload) {
    return handleProbeEvent(payload);
  }

  public Properties getConfiguration() {
    return config;
  }

  /**
   * As the Responder is multi-threaded, make sure that the access to the
   * services list which might change out if the user changes it, is synchronized to make sure
   * nothing weird happens.
   * @param services - the ArrayList of ServiceInfoBeans
   */
  public synchronized void setServiceList(ArrayList<ServiceInfoBean> services) {
    this.serviceList = services;
  }

  public synchronized ArrayList<ServiceInfoBean> getServiceList() {
    return this.serviceList;
  }

  /**
   * Handle the probe event.
   */
  public ResponsePayloadBean handleProbeEvent(ProbePayloadBean payload) {

    ResponsePayloadBean response = new ResponsePayloadBean(payload.probe.getId());

    LOGGER.fine("ConfigFileProbeHandlerPluginImpl handling probe: " + payload.toString());

    // do the actual lookup here
    // and create and return the ResponderPayload
    // Can you say O(n^2) lookup? Very bad - we can fix later

    if (payload.isNaked()) {
      LOGGER.fine("Query all detected - no service contract IDs in probe");
      for (ServiceInfoBean entry : serviceList) {
        // If the set of contract IDs is empty, get all of them
        response.addResponse(entry);
      }

    } else {
      for (String serviceContractID : payload.probe.getScids().getServiceContractID()) {
        LOGGER.fine("Looking to detect " + serviceContractID + " in entry list.");
        for (ServiceInfoBean entry : serviceList) {
          if (entry.getServiceContractID().equals(serviceContractID)) {
            // Boom Baby - we got one!!!
            response.addResponse(entry);
          }
        }
      }
      for (String serviceInstanceID : payload.probe.getSiids().getServiceInstanceID()) {
        LOGGER.fine("Looking to detect " + serviceInstanceID + " in entry list.");
        for (ServiceInfoBean entry : serviceList) {
          if (entry.getId().equals(serviceInstanceID)) {
            // Boom Baby - we got one!!!
            response.addResponse(entry);
          }
        }
      }
    }

    return response;

  }

  /**
   * Initialize the handler.
   */
  public void initializeWithPropertiesFilename(String filename) throws IOException {

    config.load(new FileInputStream(filename));

    // Launch the timer task that will look for changes to the config file
    configFileScan = new Timer();
    configFileScan.scheduleAtFixedRate(new ConfigFileMonitorTask(this), 2000, 10000);

  }

}
