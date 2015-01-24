package ws.argo.Responder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ws.argo.Responder.plugin.ConfigFileProbeHandlerPluginImpl;
import ws.argo.Responder.plugin.ProbeHandlerPluginIntf;

public class Responder {
	
	private static final String PROBE = "probe";
	private static final String XML = "XML";
	private static final String JSON = "JSON";
	
	protected MulticastSocket socket = null;
	protected InetAddress address;
	private static Options options = null;	
	
	private Set<String> handledProbes = new HashSet<String>();
	protected CloseableHttpClient httpClient;

	private static class ResponderCLIValues {
    	public ResponderCLIValues(ResponderConfigurationBean propsConfig) {
			this.config = propsConfig;
		}
		public String propertiesFilename;
    	public ResponderConfigurationBean config = new ResponderConfigurationBean();
	}
	
	private static class ResponderConfigurationBean {

		public int multicastPort;
		public String multicastAddress;
		public ArrayList<AppHandlerConfig> appHandlerConfigs = new ArrayList<AppHandlerConfig>();
    	public boolean verbose = false;
    	public boolean debug = false;
		
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
				System.out.println("Could not create an instance of the configured handler class - "+appConfig.classname);
				System.out.println("Using default handler");
				System.out.println("The issue was:");
				e.printStackTrace();
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
		socket.joinGroup(address);

		DatagramPacket packet;
		ResponsePayloadBean response = null;

		System.out.println("Responder started on "+cliValues.config.multicastAddress+":"+cliValues.config.multicastPort);
		
		httpClient = HttpClients.createDefault();

		
		// infinite loop until the responder is terminated
		while (true) {

			byte[] buf = new byte[1024];
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);

			// Get the string
			String probeStr = new String(packet.getData(), 0, packet.getLength());			
//			System.out.println("Probe: \n" + probeStr);

			try {
				ProbePayloadBean payload = parseProbePayload(probeStr);
				
				System.out.println("Received probe id: "+payload.probeID);
				
				// Only handle probes that we haven't handled before
				// The Probe Generator needs to send a stream of identical UDP packets
				// to compensate for UDP reliability issues.  Therefore, the Responder
				// will likely get more than 1 identical probe.  We should ignore duplicates.
				if (!handledProbes.contains(payload.probeID)) {		
					for (ProbeHandlerPluginIntf handler : handlers) {
						response = handler.probeEvent(payload);
						sendResponse(payload.respondToURL, payload.respondToPayloadType, response);
					}
					handledProbes.add(payload.probeID);
				} else {
					System.out.println("Discarding duplicate probe with id: "+payload.probeID);
				}

			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
		}
    	
    }
    
	private void sendResponse(String respondToURL, String payloadType, ResponsePayloadBean response) {
		
		// This method will likely need some thought and care in the error handling and error reporting
		// It's a had job at the moment.
		
		String responseStr = null;
		String contentType = null;  //MIME type
		
		switch (payloadType) {
			case "XML" : {
				responseStr = response.toXML();
				contentType = "application/xml";
				break;
			}
			case "JSON" : {
				responseStr = response.toJSON();
				contentType = "application/json";
				break;
			}
			default: responseStr = response.toJSON();
		}
	
		try {

			//DefaultHttpClient httpClient = new DefaultHttpClient();
			
			HttpPost postRequest = new HttpPost(respondToURL);

			StringEntity input = new StringEntity(responseStr);
			input.setContentType(contentType);
			postRequest.setEntity(input);

			CloseableHttpResponse httpResponse = httpClient.execute(postRequest);
			try {
		
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				if (statusCode > 300) {
					throw new RuntimeException("Failed : HTTP error code : "
							+ httpResponse.getStatusLine().getStatusCode());
				}
	
				if (statusCode != 204) {
					BufferedReader br = new BufferedReader(new InputStreamReader(
						(httpResponse.getEntity().getContent())));
	
					String output;
					System.out.println("Output from Listener .... \n");
					while ((output = br.readLine()) != null) {
						System.out.println(output);
					}
				}
			} finally {

				httpResponse.close();
			}
			
			System.out.println("Response payload sent successfully to respondTo address.");

		} catch (MalformedURLException e) {
			System.out.println("MalformedURLException occured\nThe respondTo URL was a no good.  respondTo URL is: "+respondToURL);
//			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("An IOException occured: the error message is - "+e.getMessage());
//			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Some error occured: the error message is - "+e.getMessage());
//			e.printStackTrace();
		}
		
	}


	private ProbePayloadBean parseProbePayload(String payload) throws SAXException, IOException  {

		ProbePayloadBean probePayload = new ProbePayloadBean();

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
				.newInstance();
		builderFactory.setCoalescing(false);
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		InputStream is = IOUtils.toInputStream(payload);
		Document document = builder.parse(is);

		Element probe = (Element) document.getElementsByTagName(PROBE).item(0);
		
		probePayload.probeID = probe.getAttribute("id");
		probePayload.contractID = probe.getAttribute("contractID");
		
		ArrayList<String> serviceContractIDs= new ArrayList<String>();
		
		NodeList serviceContractNodes = probe.getElementsByTagName("serviceContractID");
		
		probePayload.respondToURL = ((Element) probe.getElementsByTagName("respondTo").item(0)).getTextContent();
		probePayload.respondToPayloadType = ((Element) probe.getElementsByTagName("respondToPayloadType").item(0)).getTextContent();
		
		for (int i = 0; i < serviceContractNodes.getLength(); i++) {
			Element serviceContractID = (Element) serviceContractNodes.item(i);

			String contractID = serviceContractID.getTextContent();
			serviceContractIDs.add(contractID);

		}
		probePayload.serviceContractIDs = serviceContractIDs;
		
		return probePayload;

	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {

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

		System.out.println("Responder registering shutdown hook.");
		Runtime.getRuntime().addShutdownHook(new ResponderShutdown(responder));
		
		
		responder.run();
		
	}
	
	
	
	public static class ResponderShutdown extends Thread {
		Responder agent;
		
		public ResponderShutdown(Responder agent) {
			this.agent = agent;
		}
		public void run() {
			System.out.println("Responder shutting down port "+agent.cliValues.config.multicastPort);
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
		if (cl.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "Responder", getOptions() );
			return null;
		}
		
		ResponderConfigurationBean propsConfig = new ResponderConfigurationBean();
		ResponderCLIValues cliValues = new ResponderCLIValues(propsConfig);
		
		if (cl.hasOption("pf")) {
			String propsFilename = cl.getOptionValue("pf");
			cliValues.propertiesFilename = propsFilename;
			
			try {
				propsConfig = processPropertiesValue(propsFilename, propsConfig);
			} catch (Exception e) {
	    		System.out.println("Unable to read properties file named "+propsFilename+" due to "+e.toString()+" ");
			}
		} else {
			System.out.println("WARNING: no propoerties file specified.  Working off cli override arguments.");
		}
		
		if (cl.hasOption("debug"))
			propsConfig.debug = true;

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
				System.out.println("Overriding multicast port with command line value");
			} catch (NumberFormatException e) {
				throw new ResponderConfigException("The multicast port number - "+cl.getOptionValue("p")+" - is not formattable as an integer", e);
			}
			
		}
		
		if (cl.hasOption("a")) {
			propsConfig.multicastAddress = cl.getOptionValue("a");
			System.out.println("Overriding multicast address with command line value");
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
			int port = Integer.parseInt(prop.getProperty("mulitcastPort","4446"));
			config.multicastPort = port;
		} catch (NumberFormatException e) {
			System.out.println("Error reading port numnber from properties file.  Using default port of 4446.");
			config.multicastPort = 4446;
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
