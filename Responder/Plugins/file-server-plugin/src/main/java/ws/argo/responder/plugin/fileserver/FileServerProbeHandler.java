package ws.argo.responder.plugin.fileserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.iki.elonen.SimpleWebServer;
import ws.argo.plugin.probehandler.ProbeHandlerConfigException;
import ws.argo.plugin.probehandler.ProbeHandlerPlugin;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.response.ResponseWrapper;
import ws.argo.wireline.response.ServiceWrapper;

public class FileServerProbeHandler implements ProbeHandlerPlugin {

    private static final Logger LOGGER = LogManager.getLogger( FileServerProbeHandler.class.getName() );

    private static final String SERVICE_HOST_IP = "services.host.ip";
    private static final String SERVICE_HOST_PORT = "services.host.port";
    private static final String SERVICE_IDS_KEY = "service.ids";

    private static final String SERVICE_NAME_SUFFIX = ".name";
    private static final String SERVICE_DESCRIPTION_SUFFIX = ".description";
    private static final String SERVICE_CONTRACT_DESC_SUFFIX = ".contract.description";
    private static final String SERVICE_FILES_SUFFIX = ".files";

    private Map<String, ServiceWrapper> serviceList = null;
    private String host = null;
    private int port = 7777;
    private SimpleWebServer server = null;
    private File rootDirectory = null;
    private File serviceInstanceFile = null;

    public FileServerProbeHandler() {
        serviceList = new HashMap<String, ServiceWrapper>();
        rootDirectory = new File( System.getenv( "ARGO_HOME" ), "responder/web/" );
        serviceInstanceFile = new File( System.getenv( "ARGO_HOME" ), "/responder/data/.serviceInstance" );
    }

    @Override
    public ResponseWrapper handleProbeEvent( ProbeWrapper probe ) {
        ResponseWrapper response = new ResponseWrapper( probe.getProbeId() );
        LOGGER.debug( "Handling probe: " + probe.asXML() );

        if ( probe.isNaked() ) {
            LOGGER.debug( "Probe looking for all IDs (query all) was recieved, so returning all services" );
            for ( String id : serviceList.keySet() ) {
                response.addResponse( serviceList.get( id ) );
            }
        } else {
            for ( String id : probe.getServiceContractIDs() ) {
                LOGGER.debug( "Probe requesting services with ServiceID {}", id );
                ServiceWrapper serviceWrapper = serviceList.get( id );
                if ( serviceWrapper != null ){
                    serviceWrapper.setProbeID( probe.getProbeId() );
                    serviceWrapper.setResponseID( UUID.randomUUID().toString() );
                    if ( serviceWrapper != null ) {
                        response.addResponse( serviceWrapper );
                    }
                }
            }
        }
        return response;
    }

    @Override
    public void initializeWithPropertiesFilename( String filename ) throws ProbeHandlerConfigException {
        LOGGER.info( "Initializing FileServerProbeHandler Plugin and Web Server" );

        Properties props = getProperties( filename );
        initializeHost( props );
        initializePort( props );
        initializeServiceList( props );
        
        startWebServer();
    }

    @Override
    public String pluginName() {
        return "File Server Plugin";
    }

    private void startWebServer() {
        server = new SimpleWebServer( host, port, rootDirectory, false );
        try {
            server.start( 5000, true );
            System.out.println( "Started simple file web server listening to address [" + host + "] on port [" + port + "]" );
        } catch ( IOException e ) {
            LOGGER.error( "Could not start web server", e );
            e.printStackTrace();
        }
    }

    private void initializeServiceList( Properties props ) {

        String serviceIdString = props.getProperty( SERVICE_IDS_KEY );

        if ( StringUtils.isNotBlank( serviceIdString ) ) {
            String[] serviceIds = serviceIdString.split( "," );
            for ( String serviceId : serviceIds ) {
                ServiceWrapper serviceWrapper = createService( serviceId, props );

                serviceList.put( serviceId, serviceWrapper );
            }
        } else {
            LOGGER.warn( "There were no service IDs specified in property file, so will not respond to any probes" );
        }

    }

    private ServiceWrapper createService( String serviceId, Properties props ) {
        ServiceWrapper serviceWrapper = null;
        String serviceName = props.getProperty( serviceId + SERVICE_NAME_SUFFIX );
        String serviceDesc = props.getProperty( serviceId + SERVICE_DESCRIPTION_SUFFIX, "" ); 
        String contractDesc = props.getProperty( serviceId + SERVICE_CONTRACT_DESC_SUFFIX, "" );
        
        String fileUrl = getFileUrl( serviceId, props );
        
        if ( StringUtils.isNotBlank( serviceName ) && StringUtils.isNotBlank( fileUrl ) ) {
            // String[] files = fileList.split( "," );

            serviceWrapper = new ServiceWrapper( getInstanceId( serviceId ) );
            serviceWrapper.setServiceContractID( serviceId );

            serviceWrapper.setServiceName( serviceName );
            serviceWrapper.setDescription( serviceDesc );
            serviceWrapper.setContractDescription( contractDesc );
            serviceWrapper.setConsumability( ServiceWrapper.MACHINE_CONSUMABLE );

            serviceWrapper.addAccessPoint( null, host, String.valueOf( port ), fileUrl, null, null );

        }
        return serviceWrapper;
    }

    private String getFileUrl( String serviceId, Properties props ) {
        String filename = props.getProperty( serviceId + SERVICE_FILES_SUFFIX );
        File file = new File( rootDirectory, filename );
        if ( file.exists() ){
            return "http://" + host +":" + port + "/" + filename;
        }
        return null;
    }

    private void initializeHost( Properties props ) {
        host = props.getProperty( SERVICE_HOST_IP );
        if ( StringUtils.isBlank( host ) ) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
                if ( !isValidIP( host ) ) {
                    Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
                    while ( n.hasMoreElements() ) {
                        NetworkInterface e = n.nextElement();
                        Enumeration<InetAddress> a = e.getInetAddresses();
                        while ( a.hasMoreElements() ) {
                            InetAddress addr = a.nextElement();
                            String address = addr.getHostAddress();
                            if ( !StringUtils.startsWith( address, "127" ) ) {
                                host = address;
                                break;
                            }
                        }
                    }
                }
            } catch ( Exception e ) {
                LOGGER.error( e.getMessage(), e );
            }
        }
        if ( host != null ) {
            LOGGER.debug( "Setting host to {}", host );

        } else {
            System.out.println( "ERROR - could not find an IP address for Web Server and Argo - please configure in fileServer.prop" );
        }
    }
    
    private void initializePort( Properties props ) {
        try{
            port = Integer.parseInt( props.getProperty( SERVICE_HOST_PORT ) );
            LOGGER.debug( "Setting port for Web Server and Argo Responses to: {}", port );
        }catch( NumberFormatException e ){
            LOGGER.warn( "Could not properly parse port, defaulting to 7777:", e.getMessage() );
        }
    }

    private Properties getProperties( String filename ) {
        Properties props = null;
        File file = new File( filename );
        if ( file.exists() ) {
            try ( InputStream is = new FileInputStream( file ) ) {
                props = new Properties();
                props.load( is );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        } else {
            LOGGER.warn( "ERROR - Property file could not be read from file system / classpath {}, please make sure file exists and can be read", filename );
        }
        return props;
    }

    private String getInstanceId( String serviceId ) {
        String instanceId = null;
        if ( serviceInstanceFile.exists() ){
            try {
                String s = FileUtils.readFileToString( serviceInstanceFile );
                instanceId = StringUtils.left( s.split( System.lineSeparator() )[0], 36 );
                LOGGER.warn( "Loaded serviceInstanceID {} for this system for serviceID {}", instanceId, serviceId );
            } catch ( Exception e ) {
                LOGGER.warn( "Could not READ .serviceInstance file for service instance ID.  Each time the responder is restarted a new service instance Id will be generated", e);
            }
        }else{
            instanceId = UUID.randomUUID().toString();
            try {
                FileUtils.write( serviceInstanceFile, instanceId );
                LOGGER.warn( "Creating new serviceInstanceID {} for this system for serviceID {}", instanceId, serviceId );
            } catch ( Exception e ) {
                LOGGER.warn( "Could not WRITE .serviceInstance file for service instance ID.  Each time the responder is restarted a new service instance Id will be generated", e);
            }
        }
        if ( instanceId == null ){
            instanceId = UUID.randomUUID().toString();
            LOGGER.warn( "Generating serviceInstanceID {} for this session only for serviceID {}", instanceId, serviceId );
        }
        return instanceId;
    }

    protected boolean isValidIP( String ip ) {
        if ( !StringUtils.startsWith( ip, "127" ) && StringUtils.contains( ip, '.' ) ) {
            return true;
        }
        return false;
    }

}
