package ws.argo.MCGateway.comms;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
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

public abstract class MCastMultihome {
	private final static Logger LOGGER = Logger.getLogger(MCastMultihome.class.getName());
	protected static Options options;

	static String MULTICAST_IPV4_GROUP = "230.0.0.2";
	static Integer MULTICAST_PORT = 4003;
	static String MULTICAST_IPV6_GROUP = "FF0E::230:2";
	
	String maddr;
	Integer mport;
	List<String> networkIntfs;
	
	public MCastMultihome() {}
	
	Properties processArgs(String[] args) {
		CommandLineParser parser = new BasicParser();
		Properties cliValues = null;
		try {
			CommandLine cl = parser.parse(getOptions(), args);
			cliValues = processCommandLine(cl);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cliValues;
	}
	
	static boolean isNetworkInterfaceSuitable(NetworkInterface ni) {
		
		try {
			if (ni.isLoopback()) return false;
			if (!ni.supportsMulticast()) return false;
			if (!ni.isUp()) return false;
//			if (ni.isPointToPoint()) return false;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return true;
	}
	
	public void printHeader(Properties p) {
		StringBuffer buf = new StringBuffer();
		buf.append(this.lauchHeaderTitleString());
		buf.append(": configuration -->> ");
		
		for (Object k : p.keySet()) {
			String pname = (String)k;
			Object val = p.get(k);
			buf.append(pname+":"+val.toString());
			buf.append("  ");
		}
		
		System.out.println(buf.toString());
		System.out.println("============================================\n");
	}
	
	public void run(String[] args) throws SocketException {

		Properties cliValues = this.processArgs(args);
		if (cliValues == null)
			return;

		printHeader(cliValues);
		
		setCliValues(cliValues);

		Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
		while (ifs.hasMoreElements()) {
			NetworkInterface xface = ifs.nextElement();
			String name = xface.getName();

			if (networkIntfs.isEmpty() || networkIntfs.contains(name)) {
				if (isNetworkInterfaceSuitable(xface)) {

					launchOnNetowrkInterface(xface);
				}

			}

		}

	}
	
	@SuppressWarnings("unchecked")
	protected void setCliValues(Properties cliValues) {
		
		this.maddr = cliValues.getProperty("ma", MULTICAST_IPV4_GROUP);
		this.mport = (Integer) cliValues.get("mp");
		if (this.mport == null) this.mport = MULTICAST_PORT;
		this.networkIntfs = (List<String>) cliValues.get("il");
		if (this.networkIntfs == null) this.networkIntfs = new ArrayList<String>();
		
	}

	abstract void launchOnNetowrkInterface(NetworkInterface xface);
	abstract String lauchHeaderTitleString();

	@SuppressWarnings("static-access")
	protected static Options getStandardOptions() {
		
		Options stdOptions = new Options();
	    	
		stdOptions.addOption(new Option( "help", "print this message" ));
		stdOptions.addOption(OptionBuilder.withArgName("networkIntfs").hasArg().withDescription("comma separated list of network interface names (from ifconfig)").create("nilist"));
		stdOptions.addOption(OptionBuilder.withArgName("multicastPort").hasArg().withType(new Integer(0)).withDescription("the multicast port to send/listen on").create("mp"));
		stdOptions.addOption(OptionBuilder.withArgName("multicastAddr").hasArg().withDescription("the multicast group address to send/listen on").create("ma"));
		
		return stdOptions;
	}
	
	protected Properties processCommandLine(CommandLine cl) throws RuntimeException {

		LOGGER.config("Parsing command line values:");
		
		Properties values = new Properties();
		
		if (cl.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "GatewaySender", getOptions() );
			return null;
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
		
		//Interface List
		if (cl.hasOption("il")) {
			String il = cl.getOptionValue("nilist");
			String[] intfs = il.split("[,]");
			List<String> list = Arrays.asList(intfs);
			values.put("il", list);
		}
	
	
		return values;
		
	}	
	
	Options getOptions() {
		
		if (options == null)
	    	options = getStandardOptions();
	    	
		return options;
	}

}
