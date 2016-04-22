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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ws.argo.plugin.probehandler.ProbeHandlerConfigException;
import ws.argo.plugin.probehandler.ProbeHandlerPlugin;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.response.ResponseWrapper;
import ws.argo.wireline.response.ServiceWrapper;

/**
 * This default probe handler will load up a list of IP addresses and port
 * number associated with a service contract ID (like a UUID).
 * 
 * <p>This handler will read a config xml file which lists the services that it can
 * respond with.
 * 
 * @see ConfigFileMonitorTask
 * 
 * @author jmsimpson
 *
 */
public class ConfigFileProbeHandlerPlugin implements ProbeHandlerPlugin {

  private static final Logger LOGGER        = LogManager.getLogger(ConfigFileProbeHandlerPlugin.class.getName());

  Properties                  config        = new Properties();

  // This really needs to be better than an O(n^2) lookup - like an O(n log n)
  // with a HashMap with a list value. But I'm lazy at the moment
  ArrayList<ServiceWrapper>   serviceList   = new ArrayList<ServiceWrapper>();

  private Timer               configFileScan;

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final Lock          readLock      = readWriteLock.readLock();
  private final Lock          writeLock     = readWriteLock.writeLock();

  @Override
  public String pluginName() {
    return "Configuration File Service List";
  }

  public Properties getConfiguration() {
    return config;
  }

  /**
   * As the Responder is multi-threaded, make sure that the access to the
   * services list which might change out if the user changes it, is
   * synchronized to make sure nothing weird happens.
   * 
   * <p>There is lock on this that will always allow a read unless another thread
   * is updating the service list.
   * 
   * @param services - the ArrayList of ServiceInfoBeans
   */
  public void setServiceList(ArrayList<ServiceWrapper> services) {
    writeLock.lock();
    try {
      LOGGER.info("Updating service list by " + Thread.currentThread().getName());
      this.serviceList = services;
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Get the list of services that the handler, well, uh, handles.
   * 
   * <p>There is lock on this that will always allow a read unless another thread
   * is updating the service list.
   * 
   * @return list of services
   */
  public ArrayList<ServiceWrapper> getServiceList() {
    readLock.lock();
    try {
      return this.serviceList;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Handle the probe event.
   */
  @Override
  public ResponseWrapper handleProbeEvent(ProbeWrapper probe) {

    ResponseWrapper response = new ResponseWrapper(probe.getProbeId());

    LOGGER.debug("ConfigFileProbeHandlerPlugin handling probe: " + probe.asXML());

    // do the actual lookup here
    // and create and return the ResponderPayload
    // Can you say O(n^2) lookup? Very bad - we can fix later

    if (probe.isNaked()) {
      LOGGER.debug("Query all detected - no service contract IDs in probe");
      for (ServiceWrapper entry : serviceList) {
        // If the set of contract IDs is empty, get all of them
        response.addResponse(entry);
      }

    } else {
      for (String serviceContractID : probe.getServiceContractIDs()) {
        LOGGER.debug("Looking to detect " + serviceContractID + " in entry list.");
        for (ServiceWrapper entry : serviceList) {
          if (entry.getServiceContractID().equals(serviceContractID)) {
            // Boom Baby - we got one!!!
            response.addResponse(entry);
          }
        }
      }
      for (String serviceInstanceID : probe.getServiceInstanceIDs()) {
        LOGGER.debug("Looking to detect " + serviceInstanceID + " in entry list.");
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
  public void initializeWithPropertiesFilename(String filename) throws ProbeHandlerConfigException {

    InputStream is;
    // try to load the properties file off the classpath first

    if (ConfigFileProbeHandlerPlugin.class.getResource(filename) != null) {
      is = ConfigFileProbeHandlerPlugin.class.getResourceAsStream(filename);
    } else {
      try {
        is = new FileInputStream(filename);
      } catch (FileNotFoundException e) {
        throw new ProbeHandlerConfigException("Error loading handler config for [" + this.getClass().getName() + "]", e);
      }
    }

    try {
      config.load(is);
    } catch (IOException e) {
      throw new ProbeHandlerConfigException("Error loading handler config for [" + this.getClass().getName() + "]", e);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        throw new ProbeHandlerConfigException("Error closing handler config for {" + this.getClass().getName() + "]", e);
      }
    }

    // Launch the timer task that will look for changes to the config file
    try {
      configFileScan = new Timer();
      configFileScan.scheduleAtFixedRate(new ConfigFileMonitorTask(this), 100, 10000);
    } catch (ConfigurationException e) {
      throw new ProbeHandlerConfigException("Error initializing monitor task.", e);
    }

  }

}
