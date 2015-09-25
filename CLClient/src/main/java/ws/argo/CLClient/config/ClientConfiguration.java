package ws.argo.CLClient.config;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.validator.routines.UrlValidator;

import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientConfigException;
import ws.argo.CLClient.TransportConfig;
import ws.argo.CLClient.listener.ResponseListener;

public class ClientConfiguration {

  private UrlValidator               _urlValidator;
  private XMLConfiguration           _config;
  private ArrayList<TransportConfig> _transportConfigs = new ArrayList<TransportConfig>();

  private Map<String, String> _interpolatorMap = new HashMap<String, String>();
  private StrLookup           _lookup          = StrLookup.mapLookup(_interpolatorMap);
  private StrSubstitutor      _substitutor     = new StrSubstitutor(_lookup);

  private String _listenerURL;
  private String _responseURL;

  WebTarget _target;

  public ClientConfiguration() {
    initializeDefaults();
  }

  /**
   * 
   * @param filename
   * @throws ArgoClientConfigException
   */
  public ClientConfiguration(String filename) throws ArgoClientConfigException {
    ConfigurationInterpolator.registerGlobalLookup("resolve", _lookup);
    try {
      _config = new XMLConfiguration(filename);
    } catch (ConfigurationException e) {
      throw new ArgoClientConfigException("Error reading configuration file", e);
    }

    initializeURLValidator();
    initializeResolvers();
    initializeConfiguration();
  }

  public String getListenerURL() {
    return _listenerURL;
  }

  public WebTarget getListenerTarget() {
    return _target;
  }

  /**
   * 
   * @param listenerURL
   */
  public boolean setListenerURL(String listenerURL) {
    String newURL = _substitutor.replace(listenerURL);
    boolean changed = false;
    if (!_urlValidator.isValid(newURL)) {
      Console.error("The Response Listener URL specified is invalid. Continuing with previous value.");
    } else {
      if (!newURL.equalsIgnoreCase(_listenerURL)) {
        _listenerURL = newURL;
        _target = ClientBuilder.newClient().target(listenerURL);
        changed = true;
      }
    }
    return changed;
  }

  public String getResponseURL() {
    return _responseURL;
  }

  /**
   * 
   * @param responseURL
   */
  public void setResponseURL(String responseURL) {
    String newURL = _substitutor.replace(responseURL);
    if (!_urlValidator.isValid(newURL)) {
      Console.error("The RespondTo URL specified is invalid. Continuing with previous value.");
    } else {
      this._responseURL = newURL;
    }
  }

  public ArrayList<TransportConfig> getTransportConfigs() {
    return _transportConfigs;
  }

  private void initializeResolvers() {
    String _internaIP = _config.getString("internalIP");
    String _externaIP = _config.getString("externalIP");

    String _internalResolution = initializeResolver(_internaIP);
    String _externalResolution = initializeResolver(_externaIP);

    _interpolatorMap.put("internalIP", _internalResolution);
    _interpolatorMap.put("externalIP", _externalResolution);
  }

  private void initializeURLValidator() {
    // Sanity check on the respondToURL
    // The requirement for the respondToURL is a REST POST call, so that means
    // only HTTP and HTTPS schemes.
    // Localhost is allowed as well as a valid response destination
    String[] schemes = { "http", "https" };
    _urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
  }

  private void initializeConfiguration() {

    initializeURLs();

    initializeTransportConfigurations();

  }

  private void initializeURLs() {
    String listenerURL = _config.getString("listenerURL", ResponseListener.DEFAULT_LISTENER_URI.toString());

    if (!_urlValidator.isValid(listenerURL)) {
      listenerURL = ResponseListener.DEFAULT_LISTENER_URI.toString();
      Console.error("The Response Listener URL specified in the config file is invalid. Continuing with default.");
    }
    _listenerURL = listenerURL;
    _target = ClientBuilder.newClient().target(listenerURL);

    // RespondTo URL

    String respondToURL = _config.getString("respondToURL", listenerURL);

    if (!_urlValidator.isValid(respondToURL)) {
      respondToURL = listenerURL;
      Console.error("The respondTo URL specified in the config file is invalid. Continuing with default.");
    }
    _responseURL = respondToURL;
  }

  private void initializeTransportConfigurations() {
    // handle the list of transport information

    // You know, this might be better to do as a JSON (or such) file, but you
    // can't comment out lines in JSON

    List<HierarchicalConfiguration> transports = _config.configurationsAt("transports.transport");

    for (HierarchicalConfiguration c : transports) {
      String name = c.getString("name");
      boolean enabled = Boolean.parseBoolean(c.getString("enableOnStartup"));
      boolean usesNI = Boolean.parseBoolean(c.getString("usesNI"));
      boolean requiresMC = Boolean.parseBoolean(c.getString("requiresMulticast"));
      String classname = c.getString("classname");
      String configFilename = c.getString("configFilename");

      if (configFilename != null) {
        TransportConfig config = new TransportConfig(name);
        config.setClassname(classname);
        config.setEnabled(enabled);
        config.setUsesNetworkInterface(usesNI);
        config.setRequiresMulticast(requiresMC);
        config.setPropertiesFilename(configFilename);

        _transportConfigs.add(config);
      }
    }

    /*
     * boolean continueProcessing = true; int number = 1; while
     * (continueProcessing) {
     * 
     * String name = prop.getProperty("transportName." + number); boolean
     * enabled =
     * Boolean.parseBoolean(prop.getProperty("transportEnabledOnStartup." +
     * number)); boolean usesNI =
     * Boolean.parseBoolean(prop.getProperty("transportUsesNI." + number));
     * boolean requiresMC =
     * Boolean.parseBoolean(prop.getProperty("transportRequiresMulticast." +
     * number)); String classname = prop.getProperty("transportClassname." +
     * number); String configFilename =
     * prop.getProperty("transportConfigFilename." + number, null);
     * 
     * if (configFilename != null) { TransportConfig config = new
     * TransportConfig(name); config.setClassname(classname);
     * config.setEnabled(enabled); config.setUsesNetworkInterface(usesNI);
     * config.setRequiresMulticast(requiresMC);
     * config.setPropertiesFilename(configFilename);
     * 
     * transportConfigs.add(config); } else { continueProcessing = false; }
     * number++;
     * 
     * }
     */

  }

  private static String initializeResolver(String resolverString) {

    String ipAddress = "UNKNOWN";

    if (resolverString != null || resolverString.isEmpty()) {
      // Check to see if it's a valid URL
      ipAddress = ipAddressFromURL(resolverString);
      if (ipAddress != null) {
        return ipAddress;
      }

      // Check to see if it's a NI name
      ipAddress = ipAddressFromNI(resolverString);
      if (ipAddress != null) {
        return ipAddress;
      }

      // otherwise its a literal
      ipAddress = resolverString;

    }

    return ipAddress;

  }

  private static String ipAddressFromURL(String url) {
    String ipAddr = null;
    WebTarget resolver;
    String[] schemes = { "http", "https" };
    UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);

    if (urlValidator.isValid(url)) {
      Client resolverClient = ClientBuilder.newClient();
      resolver = resolverClient.target(url);
      try {
        ipAddr = resolver.request().get(String.class);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return ipAddr;
  }

  private static String ipAddressFromNI(String niSpec) {
    String ipAddress = null;
    NetworkInterface ni = null;
    if (niSpec.startsWith("ni:")) {
      String[] parts = niSpec.split(":");
      String niName = "eth0";
      String scheme = "ipv4";

      if (parts.length > 1) {
        niName = parts[1].toLowerCase();
      }
      if (parts.length > 2) {
        String _scheme = parts[2].toLowerCase();
        if (_scheme.equalsIgnoreCase("ipv4") || _scheme.equalsIgnoreCase("ipv6"))
          scheme = _scheme;
      }

      try {
        // if the niName is localhost, get the IP address associated with
        // localhost
        if (niName.equalsIgnoreCase("localhost")) {
          try {
            InetAddress addr = InetAddress.getLocalHost();
            ni = NetworkInterface.getByInetAddress(addr);
          } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else {
          ni = NetworkInterface.getByName(niName);
        }
      } catch (SocketException e) {
        // Something bad happened but either way it's not a valid NI name
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      // if we have a network interface, then get the right ip
      if (ni != null) {
        Enumeration<InetAddress> addrList = ni.getInetAddresses();
        do {
          InetAddress addr = addrList.nextElement();
          switch (scheme) {
            case "ipv4":
              if (addr instanceof Inet4Address) {
                ipAddress = addr.getHostAddress();
              }
              break;
            case "ipv6":
              if (addr instanceof Inet6Address) {
                ipAddress = addr.getHostAddress();
              }
              break;
            default:
              break;
          }

        } while (addrList.hasMoreElements());

      }

    }

    return ipAddress;
  }

  private void initializeDefaults() {
    // TODO Auto-generated method stub

  }

}
