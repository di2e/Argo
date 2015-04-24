package ws.argo.MCGateway;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
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


public class GatewaySender {
	private final static Logger LOGGER = Logger.getLogger(GatewaySender.class.getName());
	private static Options options;

	NetworkInterface ni = null;
	MulticastSocket inboundSocket = null;
	InetAddress maddress;
	
	
	String unicastAddress;
	Integer unicastPort;
	String multicastAddress;
	Integer multicastPort;
	String niName;
	boolean allowLoopback;
	
	public GatewaySender(Properties p) {
		this.unicastAddress = p.getProperty("ua");
		this.unicastPort = (Integer) p.get("up");
		this.multicastAddress = p.getProperty("ma");
		this.multicastPort = (Integer) p.get("mp");
		this.niName = p.getProperty("ni");
		this.allowLoopback = (boolean) p.get("l");
	}
	

	boolean joinGroup() {
		boolean success = true;
		InetSocketAddress socketAddress = new InetSocketAddress(multicastAddress, multicastPort);
		try {
			//Setup for incoming multicast requests		
			maddress = InetAddress.getByName(multicastAddress);
			
			if (niName != null)
				ni = NetworkInterface.getByName(niName);
			if (ni == null) {
				LOGGER.fine("Network Interface name not specified.  Using the NI for "+maddress);
				ni = NetworkInterface.getByInetAddress(maddress);			
			}
					
			LOGGER.info("Starting GatewaySender:  Receiving mulitcast @ "+multicastAddress+":"+multicastPort+" -- Sending unicast @ "+unicastAddress+":"+unicastPort);
			this.inboundSocket = new MulticastSocket(multicastPort);
			this.inboundSocket.joinGroup(socketAddress, ni);
			LOGGER.info(this.ni.getName()+" joined group "+socketAddress.toString());
		} catch (IOException e) {
			StringBuffer buf = new StringBuffer();
			try {
				buf.append("(lb:"+this.ni.isLoopback()+" ");
			} catch (SocketException e2) {
				buf.append("(lb:err ");
			}
			try {
				buf.append("m:"+this.ni.supportsMulticast()+" ");
			} catch (SocketException e3) {
				buf.append("(m:err ");
			}
			try {
				buf.append("p2p:"+this.ni.isPointToPoint()+" ");
			} catch (SocketException e1) {
				buf.append("p2p:err ");
			}
			try {
				buf.append("up:"+this.ni.isUp()+" ");
			} catch (SocketException e1) {
				buf.append("up:err ");
			}
			buf.append("v:"+this.ni.isVirtual()+") ");
			
			System.out.println(this.ni.getName()+" "+buf.toString()+": could not join group "+socketAddress.toString()+" --> "+e.toString());

			success = false;
		}
		return success;
	}

	
	public void run() {
				
		if (!this.joinGroup())
			return;  //If we can't join the group, end the process

		DatagramPacket packet;
		
		LOGGER.fine("Starting Gateway loop - infinite until process terminated");
		// infinite loop until the responder is terminated
		while (true) {
	
			byte[] buf = new byte[2048];
			packet = new DatagramPacket(buf, buf.length);
			LOGGER.fine("Waiting to recieve packet on "+maddress+":"+multicastPort);
			try {
				inboundSocket.receive(packet);
				new GSHandlerThread(packet, unicastAddress, unicastPort, allowLoopback).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		
	}

	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		LOGGER.info("Starting Argo GatewaySender process.");
		
		CommandLineParser parser = new BasicParser();
		Properties cliValues = null;
		try {
			CommandLine cl = parser.parse(getOptions(), args);
			cliValues = processCommandLine(cl);
			if (cliValues == null) return; //exit the program - usually from -help
		} catch (ParseException e) {
			LOGGER.severe("EXITING --> "+e.getMessage());
			return;
		}
		
		GatewaySender gateway = new GatewaySender(cliValues);

		LOGGER.fine("GatewaySender registering shutdown hook.");
		Runtime.getRuntime().addShutdownHook(new GatewaySenderShutdown(gateway));
		
		try {
			gateway.run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	
	public static class GatewaySenderShutdown extends Thread {
		GatewaySender agent;
		
		public GatewaySenderShutdown(GatewaySender agent) {
			this.agent = agent;
		}
		public void run() {
			LOGGER.fine("Gateway shutting inbound socket on "+agent.multicastPort);
			if (agent.inboundSocket != null) {
				try {
					agent.inboundSocket.leaveGroup(agent.maddress);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				agent.inboundSocket.close();
				
			}
		}
	}
	
	@SuppressWarnings("static-access")
	private static Options getOptions() {
		
		if (options == null) {
	    	options = new Options();
	    	
	    	options.addOption(new Option( "help", "print this message" ));
	    	options.addOption(OptionBuilder.withArgName("ni").hasArg().withDescription("network interface name to listen on").create("ni"));
	    	options.addOption(OptionBuilder.withArgName("loopback").withDescription("allow loopback packets - USE WITH CAUTION").create("l"));
	    	options.addOption(OptionBuilder.withArgName("multicastPort").isRequired().hasArg().withType(new Integer(0)).withDescription("the multicast port to listen on").create("mp"));
	    	options.addOption(OptionBuilder.withArgName("multicastAddr").isRequired().hasArg().withDescription("the multicast group address to listen on").create("ma"));
	    	options.addOption(OptionBuilder.withArgName("unicastPort").isRequired().hasArg().withType(new Integer(0)).withDescription("the target unicast port to send to").create("up"));
	    	options.addOption(OptionBuilder.withArgName("unicastAddr").isRequired().hasArg().withDescription("the unicast address to send to").create("ua"));
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
		
		values.put("l", false);
		if (cl.hasOption("l")) {
			values.put("l", true);
		}
		
		//Network Interface
		if (cl.hasOption("ni")) {
			String ni = cl.getOptionValue("ni");
			values.put("ni", ni);
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
		
		//MulticastAddress
		if (cl.hasOption("ua")) {
			String ua = cl.getOptionValue("ua");
			values.put("ua", ua);
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
	
}
