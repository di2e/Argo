package ws.argo.common.config.test;

import static org.junit.Assert.*;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

public class ResolvingXMLConfigTest {

  public ResolvingXMLConfigTest() {
    // TODO Auto-generated constructor stub
  }
  
  @Test
  public void testConfigTest() throws ConfigurationException, UnknownHostException {
    TestXMLConfiguration config = new TestXMLConfiguration("testConfig.xml");
    
    String localhost = InetAddress.getLocalHost().getHostAddress();
    
    assertEquals("http://"+localhost+":4009", config.getString("listenerURL"));
    assertEquals(InetAddress.getLocalHost().getHostAddress(), config.getString("internalIP"));
    
  }
  
  @Test
  public void testIndexOutOfBounds() throws ConfigurationException {
    TestXMLConfiguration config = new TestXMLConfiguration("testConfig.xml");
    
    assertEquals("INDEX OUT OF BOUNDS", config.getString("internalIP7"));
    assertEquals("INDEX OUT OF BOUNDS", config.getString("internalIP8"));
  }
  
  @Test
  public void testBadNIName() throws ConfigurationException {
    TestXMLConfiguration config = new TestXMLConfiguration("testConfig.xml");
    
    assertEquals("EMPTY LIST", config.getString("internalIP2"));
  }

  @Test
  public void testBadTypeName() throws ConfigurationException {
    TestXMLConfiguration config = new TestXMLConfiguration("testConfig.xml");
    
    assertEquals("BAD TYPE", config.getString("badTypeIP"));
    assertEquals("BAD TYPE", config.getString("explicitIP"));
  }
  
  @Test
  public void testSitelocalAddreses() throws ConfigurationException, SocketException {
    TestXMLConfiguration config = new TestXMLConfiguration("testConfig.xml");
    
    NetworkInterface ni = NetworkInterface.getByName("en0");
    
    List<InterfaceAddress> list = ni.getInterfaceAddresses();
    String siteLocalIPv4address = null;
    String siteLocalIPv6address = null;
    
    for (InterfaceAddress ia : list) {
      if (ia.getAddress().isSiteLocalAddress() && ia.getAddress() instanceof Inet4Address && siteLocalIPv4address == null) {
        siteLocalIPv4address = ia.getAddress().getHostAddress();
      }
      if (ia.getAddress().isSiteLocalAddress() && ia.getAddress() instanceof Inet6Address && siteLocalIPv6address == null) {
        siteLocalIPv6address = ia.getAddress().getHostAddress();
      }
    }
    
    assertEquals(siteLocalIPv4address, config.getString("internalIP3")); // this will break on the Jenkins build system
    assertEquals("EMPTY LIST", config.getString("internalIP4"));
    
  }
  
  @Test
  public void testLinklocalAddreses() throws ConfigurationException, SocketException {
    TestXMLConfiguration config = new TestXMLConfiguration("testConfig.xml");
    
    NetworkInterface ni = NetworkInterface.getByName("en0");
    
//    int idx = ni.getIndex();
    
    List<InterfaceAddress> list = ni.getInterfaceAddresses();
    String linkLocalIPv4address = null;
    String linkLocalIPv6address = null;
    
    for (InterfaceAddress ia : list) {
      if (ia.getAddress().isLinkLocalAddress() && ia.getAddress() instanceof Inet4Address && linkLocalIPv4address == null) {
        linkLocalIPv4address = ia.getAddress().getHostAddress();
      }
      if (ia.getAddress().isLinkLocalAddress() && ia.getAddress() instanceof Inet6Address && linkLocalIPv6address == null) {
        linkLocalIPv6address = ia.getAddress().getHostAddress();
      }
    }
    
    assertEquals("EMPTY LIST", config.getString("linklocalIPv4"));
    assertEquals(linkLocalIPv6address, config.getString("linklocalIPv6"));
    
  }
  
}
