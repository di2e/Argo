package ws.argo.probe.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeGeneratorException;

/**
 * The MulticastTransport class encapsulates the mechanics of sending the probe
 * payloads out via multicast.
 * 
 * @author jmsimpson
 *
 */
public class MulticastTransport implements Transport {

  private static final String DEFAULT_ARGO_GROUP = "230.0.0.1";

  private static final int DEFAULT_ARGO_PORT = 4003;

  private static final Logger LOGGER = Logger.getLogger(MulticastTransport.class.getName());

  public String multicastAddress;

  private NetworkInterface networkInterface = null;

  public int multicastPort;

  protected MulticastSocket outboundSocket = null;

  /**
   * Create a MulticastTransport that connects to the network interface
   * associated with localhost - see {@linkplain InetAddress#getLocalHost()}.
   * 
   * @throws ProbeGeneratorException if something goes wrong at the network
   *           layer
   */
  public MulticastTransport() throws ProbeGeneratorException {
    this(DEFAULT_ARGO_GROUP, DEFAULT_ARGO_PORT);
  }

  /**
   * Create a MulticastTransport that will attach to the specified interface.
   * 
   * @param niName - the name of the NetworkInterface - see
   *          {@linkplain NetworkInterface#getByName(String)}
   * @throws ProbeGeneratorException if something goes wrong at the network
   *           layer
   */
  public MulticastTransport(String niName) throws ProbeGeneratorException {
    this(DEFAULT_ARGO_GROUP, DEFAULT_ARGO_PORT, niName);
  }

  /**
   * Create a new MulticastTransport.
   * 
   * @param multicastAddress the multicast group
   * @param multicastPort the port
   * @throws ProbeGeneratorException if there is a problem joining the multicast
   *           group
   */
  public MulticastTransport(String multicastAddress, int multicastPort) throws ProbeGeneratorException {
    this.multicastAddress = multicastAddress;
    this.multicastPort = multicastPort;

    joinGroup(""); // This will attach to the localhost NI
    LOGGER.info("MulticastTransport ready to send on [" + multicastAddress + "]");

  }

  /**
   * Create a new MulticastTransport.
   * 
   * @param multicastAddress the multicast group
   * @param multicastPort the port
   * @param niName the Network Interface name
   * @throws ProbeGeneratorException if something goes wrong at the network
   *           layer
   */
  public MulticastTransport(String multicastAddress, int multicastPort, String niName) throws ProbeGeneratorException {
    this.multicastAddress = multicastAddress;
    this.multicastPort = multicastPort;

    joinGroup(niName);

    LOGGER.info("MulticastTransport ready to send on " + niName);

  }

  /**
   * This method attempts to join the multicast group in a particular Network
   * Interface (NI). This is useful for when inbound multicast ONLY can occur on
   * a particular interface channel. However, if there is some issue with the NI
   * name, then it attempts to join on the localhost NI. It is possible,
   * however, unlikely that this will fail as well. This happens when strange
   * things are happening to the routing tables and the presentation of the NIs
   * in the OS. This can happen with you have VPN clients or hypervisors running
   * on the host OS ... so look out.
   * 
   * @param niName the name of the Network Interface
   */
  void joinGroup(String niName) throws ProbeGeneratorException {
    InetSocketAddress socketAddress = new InetSocketAddress(multicastAddress, multicastPort);

    try {
      // Setup for incoming multicast requests
      InetAddress maddress = InetAddress.getByName(multicastAddress);

      if (niName != null)
        networkInterface = NetworkInterface.getByName(niName);
      if (networkInterface == null) {
        InetAddress localhost = InetAddress.getLocalHost();
        LOGGER.fine("Network Interface name not specified.  Using the NI for localhost " + localhost.getHostAddress());
        networkInterface = NetworkInterface.getByInetAddress(localhost);
      }

      this.outboundSocket = new MulticastSocket(multicastPort);
      if (networkInterface == null) {
        // for some reason NI is still NULL. Not sure why this happens.
        this.outboundSocket.joinGroup(maddress);
        LOGGER.warning("Unable to determine the network interface for the localhost address. Check /etc/hosts for weird entry like 127.0.1.1 mapped to DNS name.");
        LOGGER.info("Unknown network interface joined group " + socketAddress.toString());
      } else {
        this.outboundSocket.joinGroup(socketAddress, networkInterface);
        LOGGER.info(networkInterface.getName() + " joined group " + socketAddress.toString());
      }
    } catch (IOException e) {

      if (networkInterface == null) {
        throw new ProbeGeneratorException("Error attempting to joint multicast address: ", e);
      } else {

        StringBuffer buf = new StringBuffer();
        try {
          buf.append("(lb:" + networkInterface.isLoopback() + " ");
        } catch (SocketException e2) {
          buf.append("(lb:err ");
        }
        try {
          buf.append("m:" + networkInterface.supportsMulticast() + " ");
        } catch (SocketException e3) {
          buf.append("(m:err ");
        }
        try {
          buf.append("p2p:" + networkInterface.isPointToPoint() + " ");
        } catch (SocketException e1) {
          buf.append("p2p:err ");
        }
        try {
          buf.append("up:" + networkInterface.isUp() + " ");
        } catch (SocketException e1) {
          buf.append("up:err ");
        }
        buf.append("v:" + networkInterface.isVirtual() + ") ");

        throw new ProbeGeneratorException(networkInterface.getName() + " " + buf.toString() + ": could not join group " + socketAddress.toString(), e);
      }
    }
  }

  /**
   * Actually send the probe out on the wire.
   * 
   * @param probe the Probe instance that has been pre-configured
   * @throws ProbeGeneratorException if something bad happened when sending the
   *           probe
   */
  @Override
  public void sendProbe(Probe probe) throws ProbeGeneratorException {

    LOGGER.info("Sending probe [" + probe.getProbeID() + "] on network inteface [" + networkInterface.getName() + "] at port [" + multicastAddress + ":" + multicastPort + "]");
    LOGGER.finest("Probe requesting TTL of [" + probe.getHopLimit() + "]");

    try {
      String msg = probe.asXML();

      LOGGER.finest("Probe payload (always XML): \n" + msg);

      byte[] msgBytes;
      msgBytes = msg.getBytes(StandardCharsets.UTF_8);

      // send discovery string
      InetAddress group = InetAddress.getByName(multicastAddress);
      DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, group, multicastPort);
      outboundSocket.setTimeToLive(probe.getHopLimit());
      outboundSocket.send(packet);

      LOGGER.finest("Probe sent on port [" + multicastAddress + ":" + multicastPort + "]");

    } catch (IOException e) {
      throw new ProbeGeneratorException("Unable to send probe. Issue sending UDP packets.", e);
    } catch (JAXBException e) {
      throw new ProbeGeneratorException("Unable to send probe because it could not be serialized to XML", e);
    }

  }

  public void close() throws ProbeGeneratorException {
    this.outboundSocket.close();
  }

  /**
   * Return the name of the NetworkInterface this ProbeGenerator is attached to.
   * If for some reason it's null, then return UNKNOWN.
   * 
   * @return the name of the NetworkInterface
   */
  public String getNIName() {
    if (networkInterface == null) {
      return "UNKNOWN";
    } else {
      return networkInterface.getName();
    }
  }

  @Override
  public int maxPayloadSize() {
    try {
      return networkInterface.getMTU();
    } catch (SocketException e) {
      LOGGER.log(Level.FINE, "Multicast Transport maxPayloadSize reqequested.  No Network Interface assigned. Returning max int value.");
      return Integer.MAX_VALUE;
    }
  }

  /**
   * Return the description of this Transport.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Multicast Transport - ");
    buf.append(" mcast group [").append(multicastAddress).append("] - port [")
      .append(multicastPort).append("] on NI [").append(getNIName()).append("]");
    
    return buf.toString();
  }

}
