package ws.argo.MCGateway.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class MulticastResponder {

	static String multicastGroup = "230.0.0.1";
//	static String multicastGroup = "FF0E::230:1";
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
	
		MulticastSocket socket = null;
		try {
			socket = new MulticastSocket(4003);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		InetAddress address = null;
		try {
			address = InetAddress.getByName(multicastGroup);

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			socket.joinGroup(address);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		DatagramPacket packet;
		// infinite loop until the responder is terminated
		while (true) {
	
			byte[] buf = new byte[1024];
			packet = new DatagramPacket(buf, buf.length);
			System.out.println("Listinging for packet...");
			try {
				socket.receive(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
			// Get the string
			String probeStr = new String(packet.getData(), 0, packet.getLength());			
			System.out.println("Probe: \n" + probeStr);
	
					
		}
	
	}
}
