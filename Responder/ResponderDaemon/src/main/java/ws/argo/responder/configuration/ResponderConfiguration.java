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

package ws.argo.responder.configuration;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;

import ws.argo.common.config.ResolvingXMLConfiguration;
import ws.argo.responder.configuration.PluginConfig;

public class ResponderConfiguration extends ResolvingXMLConfiguration {

  private static final Logger     LOGGER = Logger.getLogger(ResponderConfiguration.class.getName());

  private boolean                 _noBrowser;
  private ArrayList<PluginConfig> _probeHandlerConfigs;
  private ArrayList<PluginConfig> _transportConfigs;
  private boolean                 _runMonitor;
  private int                     _monitorInterval;
  private int                     _threadPoolSize;

  private boolean                 _allowHTTPS;
  private String                  _truststoreType;
  private String                  _truststoreFilename;
  private String                  _truststorePassword;

  public ResponderConfiguration() {
  }

  public ResponderConfiguration(String configFilename) throws ConfigurationException {
    super(configFilename);
  }

  @Override
  protected void initializeConfiguration() {
    initializeMonitorValues();
    initializeThreadPoolValues();
    initializeProbeHandlers();
    intializeTransports();
    initializeSecurity();
  }

  private void intializeTransports() {

    _transportConfigs = new ArrayList<PluginConfig>();

    List<HierarchicalConfiguration> transports = _config.configurationsAt("transports.transport");

    for (HierarchicalConfiguration t : transports) {
      String transportClassname;
      String configFilename;

      transportClassname = t.getString("classname");
      configFilename = t.getString("configFilename");

      // if the classname is empty or null then ignore it
      if (transportClassname != null && !transportClassname.isEmpty()) {
        PluginConfig handlerConfig = new PluginConfig(transportClassname, configFilename);
        _transportConfigs.add(handlerConfig);
      } else {
        warn("Encountered a blank classname in the configuration.  Without a classname, there is no Transport to configure.");
      }
    }

  }

  private void initializeProbeHandlers() {
    // handle the list of appHandler information

    _probeHandlerConfigs = new ArrayList<PluginConfig>();

    List<HierarchicalConfiguration> probeHandlers = _config.configurationsAt("probeHandlers.probeHandler");

    for (HierarchicalConfiguration p : probeHandlers) {
      String classname;
      String configFilename;

      classname = p.getString("classname");
      configFilename = p.getString("configFilename");

      if (classname != null && !classname.isEmpty()) {
        PluginConfig handlerConfig = new PluginConfig(classname, configFilename);
        _probeHandlerConfigs.add(handlerConfig);
      }
    }

  }

  private void initializeThreadPoolValues() {
    try {
      int threadSize = Integer.parseInt(_config.getString("threadPoolSize", "10"));
      _threadPoolSize = threadSize;
    } catch (NumberFormatException e) {
      LOGGER.warning("Error reading threadPoolSize number from properties file.  Using default threadPoolSize of 10.");
      _threadPoolSize = 10;
    }
  }

  private void initializeMonitorValues() {
    _runMonitor = Boolean.parseBoolean(_config.getString("runMonitor", "false"));

    try {
      int monitorInterval = Integer.parseInt(_config.getString("monitorInterval", "5"));
      _monitorInterval = monitorInterval;
    } catch (NumberFormatException e) {
      warn("Error reading monitorInterval number from properties file.  Using default port of 5.");
      _monitorInterval = 5;
    }
  }
  
  /**
   * The initializeSecurity method will setup the necessary values from either the config file
   * or the system properties (so you can override the config file with the -D switch I guess).
   * None of this is actually used unless the "allowHTTPS" flag is set to true in the config file.
   */
  private void initializeSecurity() {
    _allowHTTPS = Boolean.parseBoolean(_config.getString("allowHTTPS", "false"));
    _truststoreType = _config.getString("truststoreType", KeyStore.getDefaultType());
    
    String defaultTruststore = System.getProperty("javax.net.ssl.trustStore");
    String defaultTruststorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
    
    _truststoreFilename = _config.getString("truststoreFilename", defaultTruststore);
    _truststorePassword = _config.getString("truststorePassword", defaultTruststorePassword);
    
    if (_truststoreFilename != null && _truststoreFilename.isEmpty())
      _truststoreFilename = null;
    if (_truststorePassword != null && _truststorePassword.isEmpty())
      _truststorePassword = null;

    
  }
  
  public boolean isHTTPSConfigured() {
    return (_allowHTTPS && _truststoreFilename != null && _truststorePassword != null);
  }

  public boolean isNoBrowser() {
    return _noBrowser;
  }

  public void setNoBrowser(boolean noBrowser) {
    this._noBrowser = noBrowser;
  }

  public ArrayList<PluginConfig> getProbeHandlerConfigs() {
    return _probeHandlerConfigs;
  }

  public void setProbeHandlerConfigs(ArrayList<PluginConfig> probeHandlerConfigs) {
    this._probeHandlerConfigs = probeHandlerConfigs;
  }

  public ArrayList<PluginConfig> getTransportConfigs() {
    return _transportConfigs;
  }

  public void setTransportConfigs(ArrayList<PluginConfig> transportConfigs) {
    this._transportConfigs = transportConfigs;
  }

  public boolean isRunMonitor() {
    return _runMonitor;
  }

  public void setRunMonitor(boolean runMonitor) {
    this._runMonitor = runMonitor;
  }

  public int getMonitorInterval() {
    return _monitorInterval;
  }

  public void setMonitorInterval(int monitorInterval) {
    this._monitorInterval = monitorInterval;
  }

  public int getThreadPoolSize() {
    return _threadPoolSize;
  }

  public void setThreadPoolSize(int threadPoolSize) {
    this._threadPoolSize = threadPoolSize;
  }

  public boolean isAllowHTTPS() {
    return _allowHTTPS;
  }

  public String getTruststoreType() {
    return _truststoreType;
  }

  public String geTruststoreFilename() {
    return _truststoreFilename;
  }

  public String getTruststorePassword() {
    return _truststorePassword;
  }

  @Override
  protected void warn(String string) {
    LOGGER.warning(string);
  }

  @Override
  protected void info(String string) {
    LOGGER.info(string);
  }

  @Override
  protected void error(String string) {
    LOGGER.severe(string);
  }

  @Override
  protected void error(String string, Throwable e) {
    LOGGER.log(Level.SEVERE, string, e);
  }
}
