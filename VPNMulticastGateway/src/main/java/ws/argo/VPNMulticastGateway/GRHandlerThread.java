package ws.argo.VPNMulticastGateway;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;

public class GRHandlerThread extends Thread {

	Socket s;
	Boolean repeat;
	MulticastSocket outboundSocket;
	InetAddress maddress;
	Integer multicastPort;


	public GRHandlerThread(Socket s, Boolean repeat, MulticastSocket outboundSocket, InetAddress maddress, Integer multicastPort) {
		super();
		this.s = s;
		this.repeat = repeat;
		this.outboundSocket = outboundSocket;
		this.multicastPort = multicastPort;
		this.maddress = maddress;
	}



	public void run() {
    	
		byte[] fullMessage = new byte[0];
		
		byte[] buffer = new byte[1024];
		int length = 0;
		
		//Read in whatever message is sent on the socket.
		System.out.println("Reading message ...");
			
		try {
			do {
				length = s.getInputStream().read(buffer);

				System.out.println("Read " + length + " bytes: ");
						

				if (length > 0) {

					byte[] destination = new byte[fullMessage.length + length];
					System.arraycopy(fullMessage, 0, destination, 0, fullMessage.length);
					System.arraycopy(buffer, 0, destination, fullMessage.length, length);

					System.out.println("New appended message: " + new String(destination));

					fullMessage = destination;
				}

			} while (length != -1); // End the loop at EOF

			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
			
		System.out.println("Received message of size "+fullMessage.length);
		if (fullMessage.length >0) {
			System.out.println("Sending message of size "+fullMessage.length+":");
			System.out.println("MESSAGE -->> "+new String(fullMessage));
			//Send out message on group address
			
			if (this.repeat) {
				System.out.println("Repeating message @ "+outboundSocket.getLocalAddress()+":"+multicastPort);

				DatagramPacket packet = new DatagramPacket(fullMessage, fullMessage.length, maddress, multicastPort);
				try {
					outboundSocket.setTimeToLive(200);
					outboundSocket.send(packet);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				System.out.println("Message successfully repeated");
				
			}
		}
					
   	
    }
	
}
