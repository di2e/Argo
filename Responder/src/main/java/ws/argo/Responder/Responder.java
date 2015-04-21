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
import java.net.MulticastSocket;
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

	protected MulticastSocket socket = null;
	protected InetAddress address;
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
		public ArrayList<AppHandlerConfig> appHandlerConfigs = new ArrayList<AppHandlerConfig>();
		
	}
	

	private static class AppHandlerConfig {
		public String classname;
		public String configFilename;
	}

	private ResponderCLIValues cliValues;
    	
    public Responder(ResponderCLIValues cliValues) {
		this.cliValues = cliValues;
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
    
    public void run() throws IOException, ClassNotFoundException {
    	
    	ArrayList<ProbeHandlerPluginIntf> handlers = new ArrayList<ProbeHandlerPluginIntf>();
    	
    	// load up the handler classes specified in the configuration parameters
    	// I hope the hander classes are in a jar file on the classpath
    	handlers = loadHandlerPlugins(cliValues.config.appHandlerConfigs);
    	
		@SuppressWarnings("resource")
		MulticastSocket socket = new MulticastSocket(cliValues.config.multicastPort);
		address = InetAddress.getByName(cliValues.config.multicastAddress);
		LOGGER.info("Starting Responder on "+address.toString()+":"+cliValues.config.multicastPort);
		socket.joinGroup(address);

		DatagramPacket packet;
		LOGGER.info("Responder started on "+cliValues.config.multicastAddress+":"+cliValues.config.multicastPort);
		
		httpClient = HttpClients.createDefault();

		LOGGER.fine("Starting Responder loop - infinite until process terminated");
		// infinite loop until the responder is terminated
		while (true) {

			
			byte[] buf = new byte[1024];
			packet = new DatagramPacket(buf, buf.length);
			LOGGER.fine("Waiting to recieve packet...");
			socket.receive(packet);

			LOGGER.fine("Received packet");
			LOGGER.fine("Packet contents:");
			// Get the string
			String probeStr = new String(packet.getData(), 0, packet.getLength());			
			LOGGER.fine("Probe: \n" + probeStr);

			//reuses the handlers and the httpClient.  Both should be threadSafe
			new ProbeHandlerThread(handlers, probeStr, httpClient).start();
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
			if (agent.socket != null) {
				try {
					agent.socket.leaveGroup(agent.address);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				agent.socket.close();
				
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

		// The app handler plugin config needs to be configured via config file and not command line
		
//		if (cl.hasOption("handler"))
//			propsConfig.pluginClassname = cl.getOptionValue("handler");
//
//		if (cl.hasOption("hcfn"))
//			propsConfig.pluginConfigFilename = cl.getOptionValue("hcfn");

		
		if (cl.hasOption("p")) {
			try {
				int portNum = Integer.parseInt(cl.getOptionValue("p"));
				propsConfig.multicastPort = portNum;
				LOGGER.info("Overriding multicast port with command line value");
			} catch (NumberFormatException e) {
				throw new ResponderConfigException("The multicast port number - "+cl.getOptionValue("p")+" - is not formattable as an integer", e);
			}
			
		}
		
		if (cl.hasOption("a")) {
			propsConfig.multicastAddress = cl.getOptionValue("a");
			LOGGER.info("Overriding multicast address with command line value");
		}    		
		
	
		return cliValues;
		
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
			LOGGER.warning("Error reading port numnber from properties file.  Using default port of 4003.");
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
	    	options.addOption(new Option( "version", "print the version information and exit" ));
	    	options.addOption(new Option( "debug", "print debugging information" )); 
	    	options.addOption(OptionBuilder.withArgName("properties").hasArg().withType(new String()).withDescription("fully qualified properties filename").create("pf"));
	    	options.addOption(OptionBuilder.withArgName("multicastPort").hasArg().withType(new Integer(0)).withDescription("the multicast port to broadcast on").create("p"));
	    	options.addOption(OptionBuilder.withArgName("multicastAddr").hasArg().withDescription("the multicast group address to broadcast on").create("a"));
	    	options.addOption(OptionBuilder.withArgName("handler").hasArg().withDescription("the classname of the DiscoveryEventHandlerPluginIntf class").create("handler"));
	    	options.addOption(OptionBuilder.withArgName("handlerConfig").hasArg().withDescription("the filename for the configuration of the plugin").create("hcfn"));
		}
		
		return options;
	}
}
