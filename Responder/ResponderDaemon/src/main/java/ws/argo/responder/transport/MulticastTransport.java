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

package ws.argo.responder.transport;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.responder.ProbeProcessor;
import ws.argo.plugin.transport.responder.Transport;
import ws.argo.responder.Responder;
import ws.argo.wireline.probe.ProbeParseException;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.probe.XMLSerializer;

/**
 * The MulticastTransport is a transport that uses Multicast as the pub/sub
 * mechanism to move probes around.
 * 
 * @author jmsimpson
 *
 */
public class MulticastTransport implements Transport {

  private static final Logger LOGGER = LogManager.getLogger(MulticastTransport.class.getName());


  private boolean           shouldRun     = true;
  private NetworkInterface  ni            = null;
  protected MulticastSocket inboundSocket = null;
  protected InetAddress     maddress;

  // Configuration items
  private String networkInterface;
  private String multicastAddress;
  private int    multicastPort;
  private int    bufferSize = 2 * 1024; // default to 2k block

  ProbeProcessor processor;

  public MulticastTransport() {
  }

  @Override
  public void run() {
    DatagramPacket packet;

    LOGGER.debug("Starting MulticastTransport listening loop - infinite until thread terminated");
    // infinite loop until the responder is terminated
    while (shouldRun) {

      byte[] buf = new byte[bufferSize * 1024];
      packet = new DatagramPacket(buf, buf.length);
      LOGGER.debug("Waiting to recieve packet...");
      try {
        inboundSocket.receive(packet);

        LOGGER.debug("Received packet");
        LOGGER.debug("Packet contents:");

        // Get the actual wireline payload
        String probeStr = new String(packet.getData(), 0, packet.getLength());
        LOGGER.debug(probeStr);

        try {
          XMLSerializer serializer = new XMLSerializer();

          ProbeWrapper probe = serializer.unmarshal(probeStr);

          processor.processProbe(probe);

        } catch (ProbeParseException e) {
          LOGGER.error( "Error parsing inbound probe payload.", e);
        }
      } catch (SocketTimeoutException toe) {
        LOGGER.debug("MulticastTransport loop timeout fired.");
      } catch (IOException e1) {
        if (shouldRun) {
          LOGGER.error("Error during MulticastTransport wireline read loop." + e1.getMessage());
        }
      }
    }
       
    LOGGER.info("MulticastTransport was terminated by flag.");
  }

  @Override
  public void initialize(ProbeProcessor p, String propertiesFilename) throws TransportConfigException {
    this.processor = p;
    processPropertiesFile(propertiesFilename);
    
    joinGroup();   
  }

  @Override
  public String transportName() {
    return this.getClass().getName();
  }

  @Override
  public void shutdown() {
    shouldRun = false;
    LOGGER.info("MulticastTransport shutting down port [" + multicastPort + "] for processor [" + processor.getRuntimeID() + "]");
    if (inboundSocket != null) {
      try {
        inboundSocket.leaveGroup(maddress);
      } catch (IOException e) {
        LOGGER.error("Error leaving multicast group", e);
      }
      inboundSocket.close();
    }
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
   * @throws TransportConfigException if there was a problem joining the group
   */
  private void joinGroup() throws TransportConfigException {
    InetSocketAddress socketAddress = new InetSocketAddress(multicastAddress, multicastPort);
    try {
      // Setup for incoming multicast requests
      maddress = InetAddress.getByName(multicastAddress);

      if (networkInterface != null) {
        ni = NetworkInterface.getByName(networkInterface);
      }
      if (ni == null) {
        InetAddress localhost = InetAddress.getLocalHost();
        LOGGER.debug("Network Interface name not specified.  Using the NI for localhost [" + localhost.getHostAddress() + "]");
        ni = NetworkInterface.getByInetAddress(localhost);
        if (ni != null && ni.isLoopback()) {
          LOGGER.warn("DEFAULT NETWORK INTERFACE IS THE LOOPBACK !!!!.");
          LOGGER.warn("Attempting to use the NI for localhost [" + ni.getName() + "] is a loopback.");
          LOGGER.warn("Please run the Responder with the -ni switch selecting a more appropriate network interface to use (e.g. -ni eth0).");
          throw new TransportConfigException("DEFAULT NETWORK INTERFACE IS THE LOOPBACK !!!!.");
        }
      }

      LOGGER.info("Starting Responder:  Receiving mulitcast @ [" + multicastAddress + ":" + multicastPort + "]");
      this.inboundSocket = new MulticastSocket(multicastPort);

      if (ni == null) { // for some reason NI is still NULL. Not sure why
        // this happens.
        this.inboundSocket.joinGroup(maddress);
        LOGGER.warn("Unable to determine the network interface for the localhost address.  Check /etc/hosts for weird entry like 127.0.1.1 mapped to DNS name.");
        LOGGER.info("Unknown network interface joined group [" + socketAddress.toString() + "]");
      } else {
        this.inboundSocket.joinGroup(socketAddress, ni);
        LOGGER.info(ni.getName() + " joined group " + socketAddress.toString());
      }
    } catch (IOException e) {
      if (ni == null) {
        LOGGER.error( "Error attempting to joint multicast address.", e);
      } else {
        StringBuffer buf = new StringBuffer();
        try {
          buf.append("(lb:" + this.ni.isLoopback() + " ");
        } catch (SocketException e2) {
          buf.append("(lb:err ");
        }
        try {
          buf.append("m:" + this.ni.supportsMulticast() + " ");
        } catch (SocketException e3) {
          buf.append("(m:err ");
        }
        try {
          buf.append("p2p:" + this.ni.isPointToPoint() + " ");
        } catch (SocketException e1) {
          buf.append("p2p:err ");
        }
        try {
          buf.append("up:" + this.ni.isUp() + " ");
        } catch (SocketException e1) {
          buf.append("up:err ");
        }
        buf.append("v:" + this.ni.isVirtual() + ") ");

        throw new TransportConfigException(ni.getName() + " " + buf.toString() + ": could not join group " + socketAddress.toString(), e);
      }
    }
  }

  private Properties processPropertiesFile(String propertiesFilename) throws TransportConfigException {
    Properties prop = new Properties();

    InputStream is = null;
    try {
      if (Responder.class.getResource(propertiesFilename) != null) {
        is = Responder.class.getResourceAsStream(propertiesFilename);
        LOGGER.info("Reading properties file [" + propertiesFilename + "] from classpath.");
      } else {
        is = new FileInputStream(propertiesFilename);
        LOGGER.info("Reading properties file [" + propertiesFilename + "] from file system.");
      }
      prop.load(is);
    } catch (FileNotFoundException e) {
      throw new TransportConfigException(e.getLocalizedMessage(), e);
    } catch (IOException e) {
      throw new TransportConfigException(e.getLocalizedMessage(), e);
    } finally {
      try {
        is.close();
      } catch (Exception e) {
        throw new TransportConfigException(e.getLocalizedMessage(), e);
      }
    }

    try {
      int port = Integer.parseInt(prop.getProperty("multicastPort", "4003"));
      multicastPort = port;
    } catch (NumberFormatException e) {
      LOGGER.warn("Error reading port number from properties file.  Using default port of 4003.");
      multicastPort = 4003;
    }

    multicastAddress = prop.getProperty("multicastAddress", "230.0.0.1");
    networkInterface = prop.getProperty("networkInterface");

    try {
      int size = Integer.parseInt(prop.getProperty("bufferSize", "2"));
      bufferSize = size;
    } catch (NumberFormatException e) {
      LOGGER.warn("Error reading bufferSize number from properties file.  Using bufferSize port of 2.");
      multicastPort = 2;
    }

    return prop;

  }

}
