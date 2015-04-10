package ws.argo.MCGateway.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Date;

import ws.argo.ProbeGenerator.Probe;
import ws.argo.ProbeGenerator.ProbeGenerator;

public class ProbeSenderThread implements Runnable {


//	static String multicastGroup = "239.255.0.1";
	static String multicastGroup = "230.0.0.2";
//	static String multicastGroup = "FF0E::230:1";

	@SuppressWarnings("resource")
	public void run() {
		// TODO Auto-generated method stub

        while (true) {
	        try {
	            
	    		ProbeGenerator gen = new ProbeGenerator(multicastGroup, 4003);
	    		Probe probe = new Probe("http://localhost:8080/AsynchListener/api/responseHandler/probeResponse", Probe.XML);
	    		
	    		probe.addServiceContractID("uuid:03d55093-a954-4667-b682-8116c417925d");
	    		
	    		gen.sendProbe(probe);
	    		
//	            byte[] buf = new byte[256];
//	            // don't wait for request...just send a quote
//
//	            String dString = "This is a message sent at "+ new Date().toString();
//	            buf = dString.getBytes();
//
//	            DatagramPacket packet;
//	            packet = new DatagramPacket(buf, buf.length, group, 4003);
//	            socket.send(packet);
	            
	            System.out.println("Sent: "+probe.asXML());

	            try {
	                Thread.sleep(5000);
	            } catch (InterruptedException e) { }
	        }
	        catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    
		
	}
	
	
}

