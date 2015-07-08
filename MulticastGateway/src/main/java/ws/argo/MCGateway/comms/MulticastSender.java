package ws.argo.MCGateway.comms;

import java.io.IOException;


public class MulticastSender {
	
	public static void main(String[] args) throws IOException {
			Thread t;
			
			t = new Thread(new MulticastSenderThread());
		
	        t.start();
	}


}
