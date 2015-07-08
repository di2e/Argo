package ws.argo.MCGateway.comms;

import java.io.IOException;


public class ProbeSender {
	
	public static void main(String[] args) throws IOException {
			Thread t;
			
			t = new Thread(new ProbeSenderThread());
		
	        t.start();
	}


}
