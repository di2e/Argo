package ws.argo.CLClient.config;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.validator.routines.UrlValidator;

public class Resolver extends StrLookup {

  // if literal matches a network interface
  // values could be URL, literal or null - null is passthrough
  private String _internal;
  private String _external;

  private Client _resolverClient;

  private String _internalIP     = "UNKNOWN";
  private String _externalIP     = "UNKNOWN";
  private String _defaultLocalIP = "UNKNOWN";

  public Resolver(String internal, String external) {
    _resolverClient = ClientBuilder.newClient();
    try {
      _defaultLocalIP = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    _internal = internal;
    _external = external;
    _internalIP = initializeResolver(_internal);
    _externalIP = initializeResolver(_external);

    if (_internalIP.equals("UNKNOWN"))
      _internalIP = _defaultLocalIP;
  }

  @Override
  public String lookup(String varName) {
    String replacement = varName;
    switch (varName) {
      case "internalIP":
        replacement = _internalIP;
        break;
      case "externalIP":
        replacement = _externalIP;
        break;
      default:
        break;
    }
    return replacement;
  }

  private String initializeResolver(String resolverString) {

    String ipAddress = "UNKNOWN";

    if (_internal != null) {
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

  private String ipAddressFromURL(String url) {
    String ipAddr = null;
    WebTarget resolver;
    String[] schemes = { "http", "https" };
    UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);

    if (urlValidator.isValid(url)) {
      resolver = _resolverClient.target(url);
      try {
        ipAddr = resolver.request().get(String.class);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return ipAddr;
  }

  private String ipAddressFromNI(String niSpec) {
    String ipAddress = null;
    NetworkInterface ni = null;
    if (_internal.startsWith("ni:")) {
      String[] parts = _internal.split(":");
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
        ni = NetworkInterface.getByName(niName);
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
