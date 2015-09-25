package ws.argo.responder.plugin.configfile;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.validator.routines.UrlValidator;

/**
 * 
 * @author jmsimpson
 *
 */
public class ClientConfiguration {

  private UrlValidator               _urlValidator;
  private XMLConfiguration           _config;

  private Map<String, String> _interpolatorMap = new HashMap<String, String>();
  private StrLookup           _lookup          = StrLookup.mapLookup(_interpolatorMap);
  private StrSubstitutor      _substitutor     = new StrSubstitutor(_lookup);

  WebTarget _target;

  public ClientConfiguration() {
  }

  /**
   * 
   * @param filename
   * @throws ArgoClientConfigException
   */
  public ClientConfiguration(String filename) throws ConfigurationException {
    ConfigurationInterpolator.registerGlobalLookup("resolve", _lookup);
      _config = new XMLConfiguration(filename);

    initializeURLValidator();
    initializeResolvers();
    initializeConfiguration();
  }

  public WebTarget getListenerTarget() {
    return _target;
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

  }

  private static String initializeResolver(String resolverString) {

    String ipAddress = "UNKNOWN";
    if (resolverString == null || resolverString.isEmpty())
      return ipAddress;

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

}
