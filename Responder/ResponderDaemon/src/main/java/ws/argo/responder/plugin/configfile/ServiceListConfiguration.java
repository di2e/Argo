package ws.argo.responder.plugin.configfile;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;

import ws.argo.responder.plugin.configfile.xml.ServicesConfiguration;
import ws.argo.responder.plugin.configfile.xml.ServicesConfiguration.Service;
import ws.argo.responder.plugin.configfile.xml.ServicesConfiguration.Service.AccessPoints.AccessPoint;
import ws.argo.responder.plugin.configfile.xml.ServicesConfiguration.Service.AccessPoints.AccessPoint.Data;
import ws.argo.wireline.response.ServiceWrapper;

/**
 * This class encapsulated the Service list for the ConfigFile Probe Handler.
 * 
 * @author jmsimpson
 *
 */
public class ServiceListConfiguration extends XMLResolvingConfiguration {

  private static final Logger LOGGER = Logger.getLogger(ServiceListConfiguration.class.getName());

  private ArrayList<ServiceWrapper> _serviceList;
  
  public ServiceListConfiguration() {
  }

  public ServiceListConfiguration(String filename) throws ConfigurationException {
    super(filename);
  }

  public ArrayList<ServiceWrapper> getServiceList() {
    return _serviceList;
  }

  @Override
  void initializeConfiguration() {
    
    _serviceList = new ArrayList<ServiceWrapper>();

    List<HierarchicalConfiguration> services = _config.configurationsAt("service");

    for (HierarchicalConfiguration service : services) {
      ServiceWrapper serviceBean = new ServiceWrapper(service.getString("[@id]"));
      serviceBean.setServiceContractID(service.getString("[@contractID]"));

      serviceBean.setServiceName(service.getString("serviceName"));
      serviceBean.setDescription(service.getString("description"));
      serviceBean.setContractDescription(service.getString("contractDescription"));
      serviceBean.setConsumability(service.getString("consumability"));
      serviceBean.setTtl(service.getString("ttl"));

      List<HierarchicalConfiguration> apList = service.configurationsAt("accessPoints.accessPoint");

      for (HierarchicalConfiguration ap : apList) {
        String xmlData = ap.getString("data");
        String dataType = ap.getString("data[@type]");
            
        String label = ap.getString("[@label]");
        String ipAddr = ap.getString("ipAddress");
        String port = ap.getString("port");
        String url = ap.getString("url");
        
        serviceBean .addAccessPoint(label, ipAddr, port, url, dataType, xmlData);
      }

      _serviceList.add(serviceBean);
      
    }

  }

  @Override
  void warn(String msg) {
    LOGGER.warning(msg);
  }

  @Override
  void info(String msg) {
    LOGGER.info(msg);
  }

  @Override
  void error(String msg) {
    LOGGER.log(Level.SEVERE, msg);
  }

  @Override
  void error(String msg, Throwable e) {
    LOGGER.log(Level.SEVERE, msg, e);
  }


}
