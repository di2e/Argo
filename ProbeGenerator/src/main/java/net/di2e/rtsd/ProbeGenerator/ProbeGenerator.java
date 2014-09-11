package net.di2e.rtsd.ProbeGenerator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ProbeGenerator {

	public String multicastAddress;
	public int multicastPort;
    protected DatagramSocket socket = null;
	
	public ProbeGenerator(String multicastAddress, int multicastPort) throws SocketException {
		this.multicastAddress = multicastAddress;
		this.multicastPort = multicastPort;
		this.socket = new DatagramSocket(4003);
	    System.out.println("Started datagram socket on port 4003");
	    
	}

	public void sendProbe(Probe probe) throws IOException {
		
		
		System.out.println("Sending probe on port "+multicastAddress+":"+multicastPort);
		
		try {
			String msg = probe.asXML();
			
			System.out.println("Probe payload type is "+probe.respondToPayloadType);		
			System.out.println("Ready to send probe: \n"+msg);

			byte[] msgBytes;
			msgBytes = msg.getBytes();
			
			//send discovery string
			InetAddress group = InetAddress.getByName(multicastAddress);
			DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, group, multicastPort);
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
