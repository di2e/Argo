package ws.argo.responder.plugin.configfile;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
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
public abstract class XMLResolvingConfiguration {

  protected UrlValidator        _urlValidator;
  protected XMLConfiguration  _config;

  protected Map<String, String> _interpolatorMap = new HashMap<String, String>();
  protected StrLookup           _lookup          = StrLookup.mapLookup(_interpolatorMap);
  protected StrSubstitutor      _substitutor     = new StrSubstitutor(_lookup);

  public XMLResolvingConfiguration() {
  }

  /**
   * 
   * @param filename
   * @throws ArgoClientConfigException
   */
  public XMLResolvingConfiguration(String filename) throws ConfigurationException {
    ConfigurationInterpolator.registerGlobalLookup("resolve", _lookup);
    _config = new XMLConfiguration(filename);

    initializeURLValidator();
    initializeResolvers();
    initializeConfiguration();
  }

  private void initializeResolvers() {
    List<HierarchicalConfiguration> resolveNames = _config.configurationsAt("resolve");

    for (HierarchicalConfiguration c : resolveNames) {
      String name = c.getString("[@name]");
      String _resolveString = c.getString("");

      String resolvedString = initializeResolver(_resolveString);
      _interpolatorMap.put(name, resolvedString);
    }
  }

  private void initializeURLValidator() {
    // Sanity check on the respondToURL
    // The requirement for the respondToURL is a REST POST call, so that means
    // only HTTP and HTTPS schemes.
    // Localhost is allowed as well as a valid response destination
    String[] schemes = { "http", "https" };
    _urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
  }

  private String initializeResolver(String resolverString) {

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

  /**
   * Resolve the IP address from a URL.
   * 
   * @param url and URL that will return an IP address
   * @return the result from the HTTP GET operations or null of an error
   */
  private String ipAddressFromURL(String url) {
    String ipAddr = null;

    if (_urlValidator.isValid(url)) {
      WebTarget resolver = ClientBuilder.newClient().target(url);
      try {
        ipAddr = resolver.request().get(String.class);
      } catch (Exception e) {
        warn("URL Cannot Resolve. The requested URL is invalid  [" + url + "]");
      }
    }
    return ipAddr;
  }

  /**
   * Resolve the IP address from the special "resolver" network interface
   * format.
   * 
   * @param niSpec the special "resolver" network interface format
   * @return either UNKNOWN, the original string or the resolved IP address
   */
  private String ipAddressFromNI(String niSpec) {
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
                ipAddress = "[" + addr.getHostAddress() + "]";
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

  /**
   * This method NEEDS to be completed. It's the main concrete initialization
   * method for the configuration.
   */
  abstract void initializeConfiguration();

  abstract void warn(String string);

  abstract void info(String string);

  abstract void error(String string);

  abstract void error(String string, Throwable e);

}
