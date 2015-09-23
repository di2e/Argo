package ws.argo.CLClient;

/**
 * Utility class to encapsulate the configuration information for a particular
 * Transport.
 * 
 * @author jmsimpson
 *
 */
public class TransportConfig {
  String  name;
  boolean enabled;
  boolean usesNetworkInterface;
  boolean requiresMulticast;
  String  classname;
  String  propertiesFilename;

  public TransportConfig(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getClassname() {
    return classname;
  }

  public void setClassname(String classname) {
    this.classname = classname;
  }

  public String getPropertiesFilename() {
    return propertiesFilename;
  }

  public void setPropertiesFilename(String propertiesFilename) {
    this.propertiesFilename = propertiesFilename;
  }

  public boolean usesNetworkInterface() {
    return usesNetworkInterface;
  }

  public void setUsesNetworkInterface(boolean usesNetworkInterface) {
    this.usesNetworkInterface = usesNetworkInterface;
  }

  public boolean requiresMulticast() {
    return requiresMulticast;
  }

  public void setRequiresMulticast(boolean requiresMulticast) {
    this.requiresMulticast = requiresMulticast;
  }


}
