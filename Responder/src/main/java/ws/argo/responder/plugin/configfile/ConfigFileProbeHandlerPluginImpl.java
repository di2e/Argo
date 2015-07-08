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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;
import java.util.logging.Logger;

import ws.argo.responder.ProbeHandlerPluginIntf;
import ws.argo.responder.ResponderConfigException;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.response.ResponseWrapper;
import ws.argo.wireline.response.ServiceWrapper;


/**
 * This default probe handler will load up a list of IP addresses and port
 * number associated with a service contract ID (like a UUID).
 * 
 * <p>
 * This handler will read a config xml file which lists the services that it can
 * respond with.
 * 
 * @see ConfigFileMonitorTask
 * 
 * @author jmsimpson
 *
 */
public class ConfigFileProbeHandlerPluginImpl implements ProbeHandlerPluginIntf {

  private static final Logger LOGGER      = Logger.getLogger(ConfigFileProbeHandlerPluginImpl.class.getName());

  Properties                  config      = new Properties();

  // This really needs to be better than an O(n^2) lookup - like an O(n log n)
  // with a HashMap with a list value. But I'm lazy at the moment
  ArrayList<ServiceWrapper>   serviceList = new ArrayList<ServiceWrapper>();

  private Timer               configFileScan;

  public Properties getConfiguration() {
    return config;
  }

  /**
   * As the Responder is multi-threaded, make sure that the access to the
   * services list which might change out if the user changes it, is
   * synchronized to make sure nothing weird happens.
   * 
   * @param services - the ArrayList of ServiceInfoBeans
   */
  public synchronized void setServiceList(ArrayList<ServiceWrapper> services) {
    this.serviceList = services;
  }

  public synchronized ArrayList<ServiceWrapper> getServiceList() {
    return this.serviceList;
  }

  /**
   * Handle the probe event.
   */
  @Override
  public ResponseWrapper handleProbeEvent(ProbeWrapper probe) {

    ResponseWrapper response = new ResponseWrapper(probe.getProbeId());

    LOGGER.fine("ConfigFileProbeHandlerPluginImpl handling probe: " + probe.asXML());

    // do the actual lookup here
    // and create and return the ResponderPayload
    // Can you say O(n^2) lookup? Very bad - we can fix later

    if (probe.isNaked()) {
      LOGGER.fine("Query all detected - no service contract IDs in probe");
      for (ServiceWrapper entry : serviceList) {
        // If the set of contract IDs is empty, get all of them
        response.addResponse(entry);
      }

    } else {
      for (String serviceContractID : probe.getServiceContractIDs()) {
        LOGGER.fine("Looking to detect " + serviceContractID + " in entry list.");
        for (ServiceWrapper entry : serviceList) {
          if (entry.getServiceContractID().equals(serviceContractID)) {
            // Boom Baby - we got one!!!
            response.addResponse(entry);
          }
        }
      }
      for (String serviceInstanceID : probe.getServiceInstanceIDs()) {
        LOGGER.fine("Looking to detect " + serviceInstanceID + " in entry list.");
        for (ServiceWrapper entry : serviceList) {
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
  public void initializeWithPropertiesFilename(String filename) throws ResponderConfigException {

    InputStream is;
    // try to load the properties file off the classpath first

    if (ConfigFileProbeHandlerPluginImpl.class.getResource(filename) != null) {
      is = ConfigFileProbeHandlerPluginImpl.class.getResourceAsStream(filename);
    } else {
      try {
        is = new FileInputStream(filename);
      } catch (FileNotFoundException e) {
        throw new ResponderConfigException("Error loading handler config for " + this.getClass().getName(), e);
      }
    }

    try {
      config.load(is);
    } catch (IOException e) {
      throw new ResponderConfigException("Error loading handler config for " + this.getClass().getName(), e);
    }

    // Launch the timer task that will look for changes to the config file
    configFileScan = new Timer();
    configFileScan.scheduleAtFixedRate(new ConfigFileMonitorTask(this), 100, 10000);

  }

}
