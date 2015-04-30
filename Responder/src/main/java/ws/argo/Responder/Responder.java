/*
 * Copyright 2015 Jeff Simpson.
 *
 * Licensed under the MIT License, (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.argo.Responder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import ws.argo.Responder.plugin.ConfigFileProbeHandlerPluginImpl;
import ws.argo.Responder.plugin.ProbeHandlerPluginIntf;


public class Responder {
	
	private final static Logger LOGGER = Logger.getLogger(Responder.class.getName());

	NetworkInterface ni = null;
	protected MulticastSocket inboundSocket = null;
	protected InetAddress maddress;
	private static Options options = null;	
	
	protected CloseableHttpClient httpClient;

	private static class ResponderCLIValues {
    	public ResponderCLIValues(ResponderConfigurationBean propsConfig) {
			this.config = propsConfig;
		}
		public ResponderConfigurationBean config = new ResponderConfigurationBean();
	}
	
	private static class ResponderConfigurationBean {

		public int multicastPort;
		public String multicastAddress;
		public boolean noBrowser = false;
		public ArrayList<AppHandlerConfig> appHandlerConfigs = new ArrayList<AppHandlerConfig>();
		public String networkInterface;
		
	}
	

	private static class AppHandlerConfig {
		public String classname;
		public String configFilename;
	}

	private ResponderCLIValues cliValues;
    	
    public Responder(ResponderCLIValues cliValues) {
		this.cliValues = cliValues;
	}
    
    
    boolean joinGroup() {
		boolean success = true;
		InetSocketAddress socketAddress = new InetSocketAddress(cliValues.config.multicastAddress, cliValues.config.multicastPort);
		try {
			//Setup for incoming multicast requests		
			maddress = InetAddress.getByName(cliValues.config.multicastAddress);
			
			if (cliValues.config.networkInterface != null)
				ni = NetworkInterface.getByName(cliValues.config.networkInterface);
			if (ni == null) {
				LOGGER.warning("Network Interface name not specified or incorrect.  Using the NI for localhost");
				ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());			
			}
					
			LOGGER.info("Starting Responder:  Receiving mulitcast @ "+cliValues.config.multicastAddress+":"+cliValues.config.multicastPort);
			this.inboundSocket = new MulticastSocket(cliValues.config.multicastPort);
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

    
    
    public void run() throws IOException, ClassNotFoundException {
    	
    	ArrayList<ProbeHandlerPluginIntf> handlers = new ArrayList<ProbeHandlerPluginIntf>();
    	
    	// load up the handler classes specified in the configuration parameters
    	// I hope the hander classes are in a jar file on the classpath
    	handlers = loadHandlerPlugins(cliValues.config.appHandlerConfigs);
    	
		if (!joinGroup()) {
			LOGGER.severe("Responder shutting down: unable to join multicast group");
		}

		DatagramPacket packet;
		LOGGER.info("Responder started on "+cliValues.config.multicastAddress+":"+cliValues.config.multicastPort);
		
		httpClient = HttpClients.createDefault();

		LOGGER.fine("Starting Responder loop - infinite until process terminated");
		// infinite loop until the responder is terminated
		while (true) {
			
			byte[] buf = new byte[1024];
			packet = new DatagramPacket(buf, buf.length);
			LOGGER.fine("Waiting to recieve packet...");
			inboundSocket.receive(packet);

			LOGGER.fine("Received packet");
			LOGGER.fine("Packet contents:");
			// Get the string
			String probeStr = new String(packet.getData(), 0, packet.getLength());			
			LOGGER.fine(probeStr);

			//reuses the handlers and the httpClient.  Both should be threadSafe
			new ProbeHandlerThread(handlers, probeStr, httpClient, cliValues.config.noBrowser).start();
		}
    	
    }
    
	public static void main(String[] args) throws IOException, ClassNotFoundException {

		LOGGER.info("Starting Argo Responder daemon process.");
		
		CommandLineParser parser = new BasicParser();
		ResponderCLIValues cliValues = null;
		
		try {
			CommandLine cl = parser.parse(getOptions(), args);
			cliValues = processCommandLine(cl);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ResponderConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Responder responder = new Responder(cliValues);

		LOGGER.info("Responder registering shutdown hook.");
		Runtime.getRuntime().addShutdownHook(new ResponderShutdown(responder));
		
		
		responder.run();
		
	}
	
	
	
	public static class ResponderShutdown extends Thread {
		Responder agent;
		
		public ResponderShutdown(Responder agent) {
			this.agent = agent;
		}
		public void run() {
			LOGGER.info("Responder shutting down port "+agent.cliValues.config.multicastPort);
			if (agent.inboundSocket != null) {
				try {
					agent.inboundSocket.leaveGroup(agent.maddress);
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Error leaving multicast group", e);
				}
				agent.inboundSocket.close();
				
			}
		}
	}
	
	private static ResponderCLIValues processCommandLine(CommandLine cl) throws ResponderConfigException {

		LOGGER.config("Parsing command line values:");
		
		if (cl.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "Responder", getOptions() );
			return null;
		}
		
		ResponderConfigurationBean propsConfig = new ResponderConfigurationBean();
		ResponderCLIValues cliValues = new ResponderCLIValues(propsConfig);
		
		if (cl.hasOption("nb")) {
			propsConfig.noBrowser = true;
			LOGGER.info("Responder started in no browser mode.");
		}
		
		//Network Interface
		if (cl.hasOption("ni")) {
			String ni = cl.getOptionValue("ni");
			propsConfig.networkInterface = ni;
		}
		
		if (cl.hasOption("pf")) {
			String propsFilename = cl.getOptionValue("pf");
			try {
				propsConfig = processPropertiesValue(propsFilename, propsConfig);
			} catch (Exception e) {
				LOGGER.warning("Unable to read properties file named "+propsFilename+" due to "+e.toString()+" ");
			}
		} else {
			LOGGER.warning("WARNING: no propoerties file specified.  Working off cli override arguments.");
		}
		
		if (cl.hasOption("debug")) {
			LOGGER.setLevel(Level.FINE);
		}
		
		if (cl.hasOption("mp")) {
			try {
				int portNum = Integer.parseInt(cl.getOptionValue("mp"));
				propsConfig.multicastPort = portNum;
				LOGGER.info("Overriding multicast port with command line value");
			} catch (NumberFormatException e) {
				throw new ResponderConfigException("The multicast port number - "+cl.getOptionValue("mp")+" - is not formattable as an integer", e);
			}
			
		}
		
		if (cl.hasOption("ma")) {
			propsConfig.multicastAddress = cl.getOptionValue("ma");
			LOGGER.info("Overriding multicast address with command line value");
		}    		
		
	
		return cliValues;
		
	}

	private ArrayList<ProbeHandlerPluginIntf> loadHandlerPlugins(ArrayList<AppHandlerConfig> configs) throws IOException, ClassNotFoundException {
		
		ClassLoader cl = ClassLoader.getSystemClassLoader();
	
		ArrayList<ProbeHandlerPluginIntf> handlers = new ArrayList<ProbeHandlerPluginIntf>();
		
		for (AppHandlerConfig appConfig : configs) {
	    	
	    	Class<?> handlerClass = cl.loadClass(appConfig.classname);
	    	ProbeHandlerPluginIntf handler;
	    	
	    	try {
				handler = (ProbeHandlerPluginIntf) handlerClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				LOGGER.warning("Could not create an instance of the configured handler class - "+appConfig.classname);
				LOGGER.warning("Using default handler");
				LOGGER.fine("The issue was:");
				LOGGER.fine(e.getMessage());
				handler = new ConfigFileProbeHandlerPluginImpl();
			}
	    	
	    	handler.setPropertiesFilename(appConfig.configFilename);
	    	
	    	handlers.add(handler);
		}
		
		return handlers;
		
	}


	private static ResponderConfigurationBean processPropertiesValue(String propertiesFilename, ResponderConfigurationBean config) throws ResponderConfigException {
		Properties prop = new Properties();
		 
		try {
			prop.load(new FileInputStream(propertiesFilename));
		} catch (FileNotFoundException e) {
			throw new ResponderConfigException("Properties file exception:", e);
		} catch (IOException e) {
			throw new ResponderConfigException("Properties file exception:", e);
		}
		 
		try {
			int port = Integer.parseInt(prop.getProperty("multicastPort","4003"));
			config.multicastPort = port;
		} catch (NumberFormatException e) {
			LOGGER.warning("Error reading port number from properties file.  Using default port of 4003.");
			config.multicastPort = 4003;
		}
		
		config.multicastAddress = prop.getProperty("multicastAddress");
		
		// handle the list of appHandler information
		
		boolean continueProcessing = true;
		int number = 1;
		while (continueProcessing) {
			String appHandlerClassname;
			String configFilename;
			
			appHandlerClassname = prop.getProperty("probeHandlerClassname."+number, "ws.argo.Responder.plugin.ConfigFileProbeHandlerPluginImpl");
			configFilename = prop.getProperty("probeHandlerConfigFilename."+number, null);
			
			if (configFilename != null) {
				AppHandlerConfig handlerConfig = new AppHandlerConfig();
				handlerConfig.classname = appHandlerClassname;
				handlerConfig.configFilename = configFilename;
				config.appHandlerConfigs.add(handlerConfig);
			} else {
				continueProcessing = false;
			}
			number++;
			
		}
		
		return config;
	
	}

	@SuppressWarnings("static-access")
	private static Options getOptions() {
		
		if (options == null) {
	    	options = new Options();
	    	
	    	options.addOption(new Option( "help", "print this message" ));
	    	options.addOption(OptionBuilder.withArgName("ni").hasArg().withDescription("network interface name to listen on").create("ni"));
	    	options.addOption(OptionBuilder.withArgName("properties").hasArg().withType(new String()).withDescription("fully qualified properties filename").create("pf"));
	    	options.addOption(OptionBuilder.withArgName("multicastPort").hasArg().withType(new Integer(0)).withDescription("the multicast port to broadcast on").create("mp"));
	    	options.addOption(OptionBuilder.withArgName("multicastAddr").hasArg().withDescription("the multicast group address to broadcast on").create("ma"));
	    	options.addOption(OptionBuilder.withArgName("noBrowser").withDescription("setting this switch will disable the responder from returnin all services to a naked probe").create("nb"));
		}
		
		return options;
	}
}
