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
import java.net.MulticastSocket;

public class ProbeGenerator {

	public String multicastAddress;
	public int multicastPort;
    protected MulticastSocket socket = null;
	
	public ProbeGenerator(String multicastAddress, int multicastPort) throws IOException {
		this.multicastAddress = multicastAddress;
		this.multicastPort = multicastPort;
		this.socket = new MulticastSocket(4003);
		
		
	    System.out.println("Started multicast socket on port 4003");
	    System.out.println("Multicast socket default TTL is "+this.socket.getTimeToLive());
	    
	}

	public void sendProbe(Probe probe) throws IOException {
		
		
		System.out.println("Sending probe on port "+multicastAddress+":"+multicastPort);
		System.out.println("Probe requesting TTL of "+probe.ttl);
		
		try {
			String msg = probe.asXML();
			
			System.out.println("Probe payload type is "+probe.respondToPayloadType);		
			System.out.println("Ready to send probe: \n"+msg);

			byte[] msgBytes;
			msgBytes = msg.getBytes();
			
			//send discovery string
			InetAddress group = InetAddress.getByName(multicastAddress);
			DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, group, multicastPort);
			socket.setTimeToLive(probe.ttl);
			socket.send(packet);
			
			System.out.println("Probe sent on port "+multicastAddress+":"+multicastPort);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}
	
	public void close() {
		this.socket.close();
	}
	
}
