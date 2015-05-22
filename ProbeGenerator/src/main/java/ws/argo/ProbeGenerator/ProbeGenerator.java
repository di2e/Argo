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

package ws.argo.ProbeGenerator;

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

public class ProbeGenerator {

	private static final String	DEFAULT_ARGO_GROUP	= "230.0.0.1";
	private static final int	DEFAULT_ARGO_PORT	= 4003;

	private final static Logger	LOGGER	           = Logger.getLogger(ProbeGenerator.class.getName());

	public String	            multicastAddress;
	public int	                multicastPort;
	protected MulticastSocket	outboundSocket	   = null;
	private boolean	            readyToSend	       = false;

	public ProbeGenerator(String multicastAddress, int multicastPort, String niName) {
		this.multicastAddress = multicastAddress;
		this.multicastPort = multicastPort;

		this.readyToSend = joinGroup(niName);
		if (this.readyToSend) {
			LOGGER.info("ProbeGenerator ready to send on " + this.outboundSocket.getInetAddress().toString());
		}
	}

	public ProbeGenerator(String niName) throws IOException {
		this(DEFAULT_ARGO_GROUP, DEFAULT_ARGO_PORT, niName);
	}

	public ProbeGenerator() throws IOException {
		this(DEFAULT_ARGO_GROUP, DEFAULT_ARGO_PORT);
	}

	public ProbeGenerator(String multicastAddress, int multicastPort) throws IOException {
		this.multicastAddress = multicastAddress;
		this.multicastPort = multicastPort;

		this.readyToSend = joinGroup("");
		if (this.readyToSend) {
			LOGGER.info("ProbeGenerator ready to send on " + multicastAddress);
		}
	}

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
			if (ni == null) { // for some reason NI is still NULL. Not sure why
				              // this happens.
				this.outboundSocket.joinGroup(maddress);
				LOGGER.warning("Unable to determine the network interface for the localhost address. Check /etc/hosts for weird entry like 127.0.1.1 mapped to DNS name.");
				LOGGER.info("Unknown network interface joined group "
				        + socketAddress.toString());
			} else {
				this.outboundSocket.joinGroup(socketAddress, ni);
				LOGGER.info(ni.getName() + " joined group "
				        + socketAddress.toString());
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
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void close() {
		this.outboundSocket.close();
	}

}
