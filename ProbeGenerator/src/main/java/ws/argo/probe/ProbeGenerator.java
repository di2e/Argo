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

package ws.argo.probe;

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

/**
 * The ProbeGenerator is the mechanism that actually sends out the wireline
 * format over UDP on the network. The ProbeGenerator can be initialized to work
 * on any multicast group and port, but it defaults to the Argo protocol group
 * and port.
 * 
 * <p>
 * The ProbeGenerator can also be initialized with a particular Network
 * Interface (NI) name. This is useful when you need to send probe UDP packets
 * on a particular network because you have specific network requirements or
 * limitations.
 * 
 * @author jmsimpson
 *
 */
public class ProbeGenerator {

  private static final String DEFAULT_ARGO_GROUP = "230.0.0.1";
  private static final int    DEFAULT_ARGO_PORT  = 4003;

  private static final Logger LOGGER             = Logger.getLogger(ProbeGenerator.class.getName());

  public String               multicastAddress;
  public int                  multicastPort;
  protected MulticastSocket   outboundSocket     = null;
  private boolean             readyToSend        = false;

  /**
   * Create a new ProbeGenerator.
   * 
   * @param multicastAddress
   *          the multicast group
   * @param multicastPort
   *          the port
   * @param niName
   *          the Network Interface name
   */
  public ProbeGenerator(String multicastAddress, int multicastPort, String niName) {
    this.multicastAddress = multicastAddress;
    this.multicastPort = multicastPort;

    this.readyToSend = joinGroup(niName);
    if (this.readyToSend)
      LOGGER.info("ProbeGenerator ready to send on " + this.outboundSocket.getInetAddress().toString());

  }

  public ProbeGenerator(String niName) throws IOException {
    this(DEFAULT_ARGO_GROUP, DEFAULT_ARGO_PORT, niName);
  }

  public ProbeGenerator() throws IOException {
    this(DEFAULT_ARGO_GROUP, DEFAULT_ARGO_PORT);
  }

  /**
   * Create a new ProbeGenerator.
   * 
   * @param multicastAddress
   *          the multicast group
   * @param multicastPort
   *          the port
   */
  public ProbeGenerator(String multicastAddress, int multicastPort) {
    this.multicastAddress = multicastAddress;
    this.multicastPort = multicastPort;

    this.readyToSend = joinGroup("");
    if (this.readyToSend)
      LOGGER.info("ProbeGenerator ready to send on " + multicastAddress);

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
   * @param niName
   *          the name of the Network Interface
   * @return true if the join was successful
   */
  boolean joinGroup(String niName) {
    boolean success = true;
    InetSocketAddress socketAddress = new InetSocketAddress(multicastAddress, multicastPort);
    NetworkInterface ni = null;
    try {
      // Setup for incoming multicast requests
      InetAddress maddress = InetAddress.getByName(multicastAddress);

      if (niName != null)
        ni = NetworkInterface.getByName(niName);
      if (ni == null) {
        InetAddress localhost = InetAddress.getLocalHost();
        LOGGER.fine("Network Interface name not specified.  Using the NI for localhost "
            + localhost.getHostAddress());
        ni = NetworkInterface.getByInetAddress(localhost);
      }

      this.outboundSocket = new MulticastSocket(multicastPort);
      if (ni == null) {
        // for some reason NI is still NULL. Not sure why this happens.
        this.outboundSocket.joinGroup(maddress);
        LOGGER.warning("Unable to determine the network interface for the localhost address. Check /etc/hosts for weird entry like 127.0.1.1 mapped to DNS name.");
        LOGGER.info("Unknown network interface joined group " + socketAddress.toString());
      } else {
        this.outboundSocket.joinGroup(socketAddress, ni);
        LOGGER.info(ni.getName() + " joined group " + socketAddress.toString());
      }
    } catch (IOException e) {

      if (ni == null) {
        LOGGER.log(Level.SEVERE, "Error attempting to joint multicast address: ", e);
      } else {

        StringBuffer buf = new StringBuffer();
        try {
          buf.append("(lb:" + ni.isLoopback() + " ");
        } catch (SocketException e2) {
          buf.append("(lb:err ");
        }
        try {
          buf.append("m:" + ni.supportsMulticast() + " ");
        } catch (SocketException e3) {
          buf.append("(m:err ");
        }
        try {
          buf.append("p2p:" + ni.isPointToPoint() + " ");
        } catch (SocketException e1) {
          buf.append("p2p:err ");
        }
        try {
          buf.append("up:" + ni.isUp() + " ");
        } catch (SocketException e1) {
          buf.append("up:err ");
        }
        buf.append("v:" + ni.isVirtual() + ") ");

        LOGGER.severe(ni.getName() + " " + buf.toString()
            + ": could not join group " + socketAddress.toString()
            + " --> " + e.toString());
      }
      success = false;
    }
    return success;
  }

  /**
   * Actually send the probe out on the wire.
   * 
   * @param probe
   *          the Probe instance that has been pre-configured
   * @throws IOException
   *           if this instance of the ProbeGenerator is not ready to send
   *           because it did not join the multicast group successfully
   */
  public void sendProbe(Probe probe) throws IOException {

    LOGGER.info("Sending probe on port " + multicastAddress + ":" + multicastPort);
    LOGGER.info("Probe requesting TTL of " + probe.ttl);

    if (!readyToSend)
      throw new IOException("ProbeGenerator not ready to send. Did not join group " + multicastAddress);

    try {
      String msg = probe.asXML();

      LOGGER.info("Probe payload (always XML): \n" + msg);

      byte[] msgBytes;
      msgBytes = msg.getBytes(StandardCharsets.UTF_8);

      // send discovery string
      InetAddress group = InetAddress.getByName(multicastAddress);
      DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, group, multicastPort);
      outboundSocket.setTimeToLive(probe.ttl);
      outboundSocket.send(packet);

      LOGGER.info("Probe sent on port " + multicastAddress + ":" + multicastPort);

    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Unable to send probe. Issue sending UDP packets.", e);
    } catch (JAXBException e) {
      LOGGER.log(Level.SEVERE, "Unable to send probe because it could not be serialized to XML", e);
    }

  }

  public void close() {
    this.outboundSocket.close();
  }

}
