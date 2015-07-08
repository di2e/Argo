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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import ws.argo.responder.plugin.configfile.xml.ServicesConfiguration;
import ws.argo.responder.plugin.configfile.xml.ServicesConfiguration.Service;
import ws.argo.responder.plugin.configfile.xml.ServicesConfiguration.Service.AccessPoints.AccessPoint;
import ws.argo.responder.plugin.configfile.xml.ServicesConfiguration.Service.AccessPoints.AccessPoint.Data;
import ws.argo.wireline.response.ServiceWrapper;

/**
 * The ConfigFileMonitorTask will periodically check to see if the config file
 * it's looking at has changed. This is in support of having a running responder
 * where the administrator changes the config file and doens't want to restart
 * the responder. This class will pick up those changes and then re-initialize
 * the ConfigFile handler plugin with the new configuration atomically.
 * 
 * <p>
 * This class also will read the config file initially - there is no other
 * mechanism to read the config file.
 * 
 * @see ConfigFileProbeHandlerPluginImpl
 * @author jmsimpson
 *
 */
public class ConfigFileMonitorTask extends TimerTask {

  private static final Logger      LOGGER       = Logger.getLogger(ConfigFileMonitorTask.class.getName());

  ConfigFileProbeHandlerPluginImpl plugin;
  Date                             lastTimeRead = null;
  File                             xmlConfigFile;

  /**
   * Creates a monitor task with the plugin instance as a parameter. The monitor
   * needs access to the plugin in order to get its config and set the service
   * list
   * 
   * @param configFileProbeHandlerPluginImpl
   * 
   */
  public ConfigFileMonitorTask(ConfigFileProbeHandlerPluginImpl configFileProbeHandlerPluginImpl) {
    plugin = configFileProbeHandlerPluginImpl;
    Properties config = plugin.getConfiguration();
    String xmlConfigFilename = config.getProperty("xmlConfigFilename");
    xmlConfigFile = new File(xmlConfigFilename);
  }

  /**
   * When the timer goes off, look for changes to the specified xml config file.
   * For comparison, we are only interested in the last time we read the file. A
   * null value for lastTimeRead means that we never read the file before.
   */
  public void run() {
    LOGGER.fine("begin scan for config file changes ...");
    try {
      Date lastModified = new Date(xmlConfigFile.lastModified());

      if (lastTimeRead == null || lastModified.after(lastTimeRead)) {
        LOGGER.info("loading config file changes ...");
        this.loadServiceConfigFile();
        lastTimeRead = new Date();
      }
    } catch (JAXBException e) {
      LOGGER.log(Level.SEVERE, "Error parsing xml configuation file: ", e);
    } catch (FileNotFoundException e) {
      LOGGER.log(Level.SEVERE, "Error loading configuation file: ", e);
    }
    LOGGER.fine("finish scan for config file changes");
  }

  /**
   * actually load the xml configuration file. If anything goes wrong throws an
   * exception. This created a list of service records. When done, set the
   * service record list in the plugin. The plugin set function should be
   * synchronized so that it won't squash any concurrent access to the old set
   * of services.
   * 
   * @throws JAXBException if there is some issue with the XML
   * @throws FileNotFoundException if the file name does not exist
   */
  private void loadServiceConfigFile() throws JAXBException, FileNotFoundException {

    Properties config = plugin.getConfiguration();
    String xmlConfigFilename = config.getProperty("xmlConfigFilename");

    ServicesConfiguration services = parseConfigFile(xmlConfigFilename);

    ArrayList<ServiceWrapper> serviceList = constructServiceList(services);

    LOGGER.fine("Setting the service list in the plugin");
    plugin.setServiceList(serviceList);

  }

  private ArrayList<ServiceWrapper> constructServiceList(ServicesConfiguration services) {
    ArrayList<ServiceWrapper> serviceList = new ArrayList<ServiceWrapper>();

    for (Service service : services.getService()) {

      ServiceWrapper serviceBean = new ServiceWrapper(service.getId());

      serviceBean.setServiceName(service.getServiceName());
      serviceBean.setDescription(service.getDescription());
      serviceBean.setContractDescription(service.getContractDescription());
      serviceBean.setConsumability(service.getConsumability());
      serviceBean.setServiceContractID(service.getContractID());
      serviceBean.setTtl(service.getTtl());

      List<AccessPoint> apList = service.getAccessPoints().getAccessPoint();

      for (AccessPoint ap : apList) {
        Data xmlData = ap.getData();
        serviceBean
            .addAccessPoint(ap.getLabel(), ap.getIpAddress(), ap.getPort(), ap.getUrl(), xmlData
                .getType(), xmlData.getValue());
      }

      serviceList.add(serviceBean);
    }
    return serviceList;
  }

  private ServicesConfiguration parseConfigFile(String xmlConfigFilename) throws JAXBException, FileNotFoundException {
    JAXBContext jaxbContext = JAXBContext.newInstance(ServicesConfiguration.class);
    LOGGER.info("Loading configuration from " + xmlConfigFilename);
    
    InputStream is;
    // try to load the properties file off the classpath first

    if (ConfigFileProbeHandlerPluginImpl.class.getResource(xmlConfigFilename) != null) {
      is = ConfigFileProbeHandlerPluginImpl.class.getResourceAsStream(xmlConfigFilename);
    } else {
      is = new FileInputStream(xmlConfigFilename);
    }

    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    ServicesConfiguration services = (ServicesConfiguration) jaxbUnmarshaller.unmarshal(is);
    return services;
  }
}
