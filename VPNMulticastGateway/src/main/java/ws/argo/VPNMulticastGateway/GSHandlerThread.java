package ws.argo.VPNMulticastGateway;

import java.net.*;
import java.util.logging.Logger;
import java.io.*;

public class GSHandlerThread extends Thread {
	private final static Logger LOGGER = Logger.getLogger(GSHandlerThread.class.getName());

    private DatagramPacket packet = null;
    private Socket outboundSocket = null;
	String unicastAddress;
	Integer unicastPort;

    
    public GSHandlerThread(DatagramPacket packet, String ua, Integer up) {
        super("GSMulticastSenderHandlerThread");
        this.packet = packet;
        this.unicastAddress = ua;
        this.unicastPort = up;
    }


    
    public void run() {
 
		try {
			InetAddress source = packet.getAddress();
			
			//Get localhost IP address
			InetAddress localhostIP = InetAddress.getLocalHost();
			
			// if this packet is sourced locally, then don't send it.  This avoids loopbacks with a 2-way gateway.
			// The downside of this is that a 2-way gateway cannot have a probe generator (Argo client) on it.
			boolean isFromLocalhost = source.equals(localhostIP);
			if (isFromLocalhost) {
				LOGGER.info("Ignoring packed sourced from localhost");
				return;  
			}
			
			//Setup for outbound unicast
			//Connect to the remote gateway
			outboundSocket = new Socket(unicastAddress, unicastPort.intValue());
			
			LOGGER.fine("Received packet");
			LOGGER.fine("Packet contents:");
			// Get the string
			String xferPacket = new String(packet.getData(), 0, packet.getLength());			
			LOGGER.info("xferPacket: \n" + xferPacket);

			try {
				outboundSocket.getOutputStream().write(packet.getData(), 0, packet.getLength());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				outboundSocket.close();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

    }
	
}
