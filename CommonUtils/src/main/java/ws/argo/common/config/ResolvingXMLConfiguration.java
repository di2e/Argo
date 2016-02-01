package ws.argo.common.config;

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
 * The ResolvingXMLConfiguration allows for the custom resolution of ip addresses.
 * 
 * @author jmsimpson
 *
 */
public abstract class ResolvingXMLConfiguration {

  public static final String URI_TYPE = "uri";
  public static final String NI_TYPE  = "ni";

  /**
   * Scopes that can be selected to differentiate ip address scopes.
   * 
   * @author jmsimpson
   *
   */
  public static enum Scope {
    global, linklocal, sitelocal
  }

  /**
   * IP address schemes.
   * 
   * @author jmsimpson
   *
   */
  public static enum Scheme {
    ipv4, ipv6
  }

  protected UrlValidator        _urlValidator;
  protected XMLConfiguration    _config;

  protected Map<String, String> _interpolatorMap = new HashMap<String, String>();
  protected StrLookup           _lookup          = StrLookup.mapLookup(_interpolatorMap);
  protected StrSubstitutor      _substitutor     = new StrSubstitutor(_lookup);

  public ResolvingXMLConfiguration() {
  }

  /**
   * Construct a new XML Configuration given the filename.
   * 
   * @param filename xml config file
   * @throws ConfigurationException if something goes wonky
   */
  public ResolvingXMLConfiguration(String filename) throws ConfigurationException {
    ConfigurationInterpolator.registerGlobalLookup("resolveIP", _lookup);
    _config = new XMLConfiguration(filename);

    initializeURLValidator();
    initializeResolvers();
    initializeConfiguration();
  }

  private void initializeResolvers() {
    List<HierarchicalConfiguration> resolveNames = _config.configurationsAt("resolveIP");

    for (HierarchicalConfiguration c : resolveNames) {
      String name = c.getString("[@name]");
      String type = c.getString("[@type]");
      String _resolveString = c.getString("");
      
      if (_resolveString.isEmpty())
        _resolveString = "localhost";

      String resolvedString = resolve(_resolveString, type, name);
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

  private String resolve(String resolverString, String type, String name) {

    String ipAddress = "UNKNOWN";

    if (type == null) {
      warn("The type for resolveIP named [" + name + "] was not specified.  Treating value as literal");
      return "BAD TYPE";
    }

    if (resolverString != null && !resolverString.isEmpty()) {

      switch (type) {
        case NI_TYPE:
          // Check to see if it's a valid URL
          ipAddress = ipAddressFromNI(resolverString, name);
          break;
        case URI_TYPE:
          ipAddress = ipAddressFromURL(resolverString, name);
          break;

        default:
          warn("The type [" + type + "] specified for resolveIP named [" + name + "] is unknown.  Use ni or uri.  Treating value as literal.");
          ipAddress = "BAD TYPE"; // otherwise its bad type
      }

    }

    return ipAddress;

  }

  /**
   * Resolve the IP address from a URL.
   * 
   * @param url and URL that will return an IP address
   * @param name the name of the variable
   * @return the result from the HTTP GET operations or null of an error
   */
  private String ipAddressFromURL(String url, String name) {
    String ipAddr = null;

    if (_urlValidator.isValid(url)) {
      WebTarget resolver = ClientBuilder.newClient().target(url);
      try {
        ipAddr = resolver.request().get(String.class);
      } catch (Exception e) {
        warn("URL Cannot Resolve for resolveIP named [" + name + "]. The requested URL is invalid  [" + url + "].");
      }
    } else {
      warn("The requested URL for resolveIP named [" + name + "] is invalid  [" + url + "].");
    }
    return ipAddr;
  }

  /**
   * Resolve the IP address from the special "resolver" network interface
   * format.
   * 
   * @param niSpec the special "resolver" network interface format
   * @param name the name of the resolveIP item
   * @return either UNKNOWN, the original string or the resolved IP address
   */
  private String ipAddressFromNI(String niSpec, String name) {

    String result = "UNKNOWN";

    NetworkInterface ni = null;

    String[] parts = niSpec.split(":");

    String niName = "eth0"; // default NIC name
    Scheme scheme = Scheme.ipv4;
    int index = 0; // default index
    Scope scope = Scope.global; // can be global, linklocal or sitelocal - is
    // global by default

    // Parse up the spec
    for (int idx = 0; idx < parts.length; idx++) {
      switch (idx) {
        case 0:
          niName = parts[idx];
          break;
        case 1:
          String _schemeStr = parts[idx].toLowerCase();
          try {
            scheme = Scheme.valueOf(_schemeStr);
          } catch (Exception e) {
            warn("Error parsing scheme for resolveIP named [" + name + "]. Expecting ipv4 or ipv6 but got [" + _schemeStr + "].  Using default of ipv4.");
            scheme = Scheme.ipv4; // default
          }
          break;
        case 2:
          String scopeTarget = parts[idx].toLowerCase();
          try {
            scope = Scope.valueOf(scopeTarget);
          } catch (Exception e) {
            warn("Error parsing scope for resolveIP named [" + name + "]. Expecting global, sitelocal or linklocal but got [" + scopeTarget + "].  Using default of global.");
            scope = Scope.global; // default
          }
          break;
        case 3:
          try {
            index = Integer.parseInt(parts[idx]);
          } catch (NumberFormatException e) {
            index = 0; // default
          }
          break;
        default:
          break;

      }
    }

    // Find the specified NIC
    try {
      // if the niName is localhost, get the IP address associated with
      // localhost
      if (niName.equalsIgnoreCase("localhost")) {
        if (scope != Scope.sitelocal) {
          warn("resolveIP named [" + name + "] has ni of localhost and will default to scope of sitelocal (or it won't work).  Expects sitelocal but got [" + scope + "].");
          scope = Scope.sitelocal; // force scope to site local
        }
        try {
          InetAddress addr = InetAddress.getLocalHost();
          ni = NetworkInterface.getByInetAddress(addr);
        } catch (UnknownHostException e) {
          // This should not happen
          warn("The lookup of the NI for localhost for resolveIP named [" + name + "] caused an exception.  Look for odd entries in /etc/hosts.");
          return "UNKNOWN NI";
        }
      } else {
        ni = NetworkInterface.getByName(niName);
      }
    } catch (SocketException e) {
      error("An error occured looking up the interface named [" + niName + "] for resolveIP named [" + name + "]", e);
      return "UNKNOWN NI";
    }

    // if we have a network interface, then get the right ip
    List<InetAddress> ipv4Addrs = new ArrayList<InetAddress>();
    List<InetAddress> ipv6Addrs = new ArrayList<InetAddress>();
    if (ni != null) {
      // group the two types of addresses
      Enumeration<InetAddress> addrList = ni.getInetAddresses();
      do {
        InetAddress addr = addrList.nextElement();
        // filter out only the type specified (linklocal, sitelocal or global)

        switch (scope) {
          case linklocal:
            if (addr.isLinkLocalAddress()) {
              if (addr instanceof Inet4Address)
                ipv4Addrs.add((Inet4Address) addr);
              if (addr instanceof Inet6Address)
                ipv6Addrs.add((Inet6Address) addr);
            }
            break;
          case sitelocal:
            if (addr.isSiteLocalAddress()) {
              if (addr instanceof Inet4Address)
                ipv4Addrs.add((Inet4Address) addr);
              if (addr instanceof Inet6Address)
                ipv6Addrs.add((Inet6Address) addr);
            }
            break;
          case global:
            if (!addr.isSiteLocalAddress() && !addr.isLinkLocalAddress()) {
              if (addr instanceof Inet4Address)
                ipv4Addrs.add((Inet4Address) addr);
              if (addr instanceof Inet6Address)
                ipv6Addrs.add((Inet6Address) addr);
            }
            break;
          default:
            break;
        }
      } while (addrList.hasMoreElements());
    }

    List<InetAddress> targetAddrs = null;

    switch (scheme) {
      case ipv4:
        targetAddrs = ipv4Addrs;
        break;
      case ipv6:
        targetAddrs = ipv6Addrs;
        break;
      default:
        break;
    }

    // Get a candidate addr from the list
    InetAddress candidateAddr = null;
    if (!targetAddrs.isEmpty()) {
      if (index < targetAddrs.size()) {
        candidateAddr = targetAddrs.get(index);
        result = candidateAddr.getHostAddress();
      } else {
        error("Error getting index [" + index + "] addrees for resolveIP named [" + name + "]. Index is out of bounds.");
        return "INDEX OUT OF BOUNDS";
      }
    } else {
      error("Empty list of addresses for resolveIP named [" + name + "]");
      return "EMPTY LIST";
    }

    return result;

  }

  /**
   * This method NEEDS to be completed. It's the main concrete initialization
   * method for the configuration.
   */
  protected abstract void initializeConfiguration() throws ConfigurationException;

  protected abstract void warn(String string);

  protected abstract void info(String string);

  protected abstract void error(String string);

  protected abstract void error(String string, Throwable e);

}
