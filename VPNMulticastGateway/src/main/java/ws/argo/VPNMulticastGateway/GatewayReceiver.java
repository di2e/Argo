package ws.argo.VPNMulticastGateway;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class GatewayReceiver {
	private final static Logger LOGGER = Logger.getLogger(GatewayReceiver.class.getName());
	private static Options options;	
	
	NetworkInterface ni = null;
	ServerSocket inboundSocket = null;
	InetAddress uaddress;
	
	MulticastSocket outboundSocket = null;
	InetAddress maddress;
	
	Integer unicastPort;
	String multicastAddress;
	Integer multicastPort;
	Boolean repeat = true;
	String niName;
	
	public GatewayReceiver(Properties p) {
		this.unicastPort = (Integer) p.get("up");
		this.multicastAddress = p.getProperty("ma");
		this.multicastPort = (Integer) p.get("mp");
		this.repeat = (Boolean) p.get("r");
		if (this.repeat == null) this.repeat = true;
		this.niName = p.getProperty("ni");
	}	
	
	
	
	public void run() throws Exception {

		//Setup for outbound multicast 		
		maddress = InetAddress.getByName(multicastAddress);
		InetSocketAddress socketAddress = new InetSocketAddress(maddress, multicastPort);
		outboundSocket = new MulticastSocket(multicastPort.intValue());

		ni = NetworkInterface.getByName(niName);
		if (ni == null) {
			ni = NetworkInterface.getByInetAddress(maddress);			
		}
		
		LOGGER.info("Starting GatewayReceiver:  Receiving unicast @ "+unicastPort+" -- Sending mulitcast @ "+multicastAddress+":"+multicastPort);
		outboundSocket.joinGroup(socketAddress, ni);

		//Setup for inbound unicast
		//Connect to the remote gateway
		inboundSocket = new ServerSocket(unicastPort.intValue());
		
		LOGGER.fine("Starting Gateway loop - infinite until process terminated");
		// infinite loop until the responder is terminated
		while (true) {
			
			System.out.println("Listening for message ...");
			Socket s = inboundSocket.accept();
			
			new GRHandlerThread(s, repeat, outboundSocket, maddress, multicastPort).start();
		}
				
	}
	
	public static void main(String[] args) throws Exception {
		LOGGER.info("Starting Argo GatewayReceiver process.");
		
		CommandLineParser parser = new BasicParser();
		Properties cliValues = null;
		try {
			CommandLine cl = parser.parse(getOptions(), args);
			cliValues = processCommandLine(cl);
			if (cliValues == null) return; //exit the program - usually from -help
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		GatewayReceiver gateway = new GatewayReceiver(cliValues);

		LOGGER.info("GatewaySender registering shutdown hook.");
		Runtime.getRuntime().addShutdownHook(new GatewaySenderShutdown(gateway));
		
		
		try {
			gateway.run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}	

	
	@SuppressWarnings("static-access")
	private static Options getOptions() {
		
		if (options == null) {
	    	options = new Options();
	    	
	    	options.addOption(new Option( "help", "print this message" ));
//	    	options.addOption(OptionBuilder.withArgName("properties").hasArg().withType(new String()).withDescription("fully qualified properties filename").create("pf"));
	    	options.addOption(OptionBuilder.withArgName("ni").hasArg().withType(new Integer(0)).withDescription("network interface name to listen on").create("ni"));
	    	options.addOption(OptionBuilder.withArgName("repeat").hasArg().withType(new Integer(0)).withDescription("should receiver repeat message").create("r"));
	    	options.addOption(OptionBuilder.withArgName("multicastPort").hasArg().withType(new Integer(0)).withDescription("the multicast port to listen on").create("mp"));
	    	options.addOption(OptionBuilder.withArgName("multicastAddr").hasArg().withDescription("the multicast group address to listen on").create("ma"));
	    	options.addOption(OptionBuilder.withArgName("unicastPort").hasArg().withDescription("the target unicast port to send to").create("up"));
		}
		
		return options;
	}
	
	private static Properties processCommandLine(CommandLine cl) throws RuntimeException {

		LOGGER.config("Parsing command line values:");
		
		Properties values = new Properties();
		
		if (cl.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "GatewaySender", getOptions() );
			return null;
		}
		
		//Network Interface
		if (cl.hasOption("ni")) {
			String ni = cl.getOptionValue("ni");
			values.put("ni", ni);
		}
		
		//Message Repeat
		if (cl.hasOption("r")) {
			String rs = cl.getOptionValue("r");
			Boolean r = Boolean.valueOf(rs);
			values.put("r", r);
		}
		
		//MulticastAddress
		if (cl.hasOption("ma")) {
			String ma = cl.getOptionValue("ma");
			values.put("ma", ma);
		}
		
		//MulticastPort
		if (cl.hasOption("mp")) {
			try {
				Integer portNum = Integer.valueOf(cl.getOptionValue("mp"));
				values.put("mp", portNum);
			} catch (NumberFormatException e) {
				throw new RuntimeException("The multicast port number - "+cl.getOptionValue("mp")+" - is not formattable as an integer", e);
			}
		}
		
		
		//MulticastPort
		if (cl.hasOption("up")) {
			try {
				Integer portNum = Integer.valueOf(cl.getOptionValue("up"));
				values.put("up", portNum);
			} catch (NumberFormatException e) {
				throw new RuntimeException("The unicast port number - "+cl.getOptionValue("up")+" - is not formattable as an integer", e);
			}
		}
		  		
		
	
		return values;
		
	}	
	
	public static class GatewaySenderShutdown extends Thread {
		GatewayReceiver agent;
		
		public GatewaySenderShutdown(GatewayReceiver agent) {
			this.agent = agent;
		}
		public void run() {
			LOGGER.info("Gateway shutting outbound socket on "+agent.multicastPort);
			if (agent.outboundSocket != null) {
				try {
					agent.outboundSocket.leaveGroup(agent.maddress);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				agent.outboundSocket.close();
				
			}
			LOGGER.info("Gateway shutting inbound socket on "+agent.unicastPort);
			if (agent.inboundSocket != null) {
				try {
					agent.inboundSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
