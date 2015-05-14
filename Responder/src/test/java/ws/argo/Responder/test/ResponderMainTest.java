package ws.argo.Responder.test;

import java.io.IOException;

import org.junit.Test;

import ws.argo.Responder.Responder;

public class ResponderMainTest {

		
	@Test
	public void testResponderHelp() throws ClassNotFoundException, IOException {
		
		String[] args = new String[1];
		args[0] = "-h"; //help
		Responder.main(args); 
		
	}
	
	@Test
	public void testResponderBadArg() throws ClassNotFoundException, IOException {
		
		String[] args = new String[1];
		args[0] = "-xx"; //help
		Responder.main(args); 
		
	}
	
	
	
	
}
