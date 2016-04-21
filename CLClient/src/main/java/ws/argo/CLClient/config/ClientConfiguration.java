package ws.argo.CLClient.config;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import net.dharwin.common.tools.cli.api.console.Console;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.http.annotation.Immutable;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;

import ws.argo.CLClient.TransportConfig;
import ws.argo.CLClient.listener.ResponseListener;
import ws.argo.common.config.ResolvingXMLConfiguration;

/**
 * ClientConfiguration encapsulated the configuration needed to run the Argo
 * Client.
 * 
 * @author jmsimpson
 *
 */
public class ClientConfiguration extends ResolvingXMLConfiguration {

  /**
   * The NO_OP HostnameVerifier essentially turns hostname verification
   * off. This implementation is a no-op, and never throws the SSLException.
   *
   * @since 4.4
   */
  @Immutable
  public static class NoopHostnameVerifier implements HostnameVerifier {

      public static final NoopHostnameVerifier INSTANCE = new NoopHostnameVerifier();

      @Override
      public boolean verify(final String s, final SSLSession sslSession) {
          return true;
      }

      @Override
      public final String toString() {
          return "NO_OP";
      }

  }
  
  private ArrayList<TransportConfig> _transportConfigs;
  private String                     _listenerURL;
  private String                     _responseURL;

  private SSLContextConfigurator     _sslContextConfigurator;
  private boolean                    _allowHTTPS;
  private String                     _keystore;
  private String                     _ksPassword;
  private String                     _truststore;
  private String                     _tsPassword;

  WebTarget                          _listenerTarget;

  public ClientConfiguration() {
    initializeDefaults();
  }

  public ClientConfiguration(String filename) throws ConfigurationException {
    super(filename);
  }

  @Override
  public void initializeConfiguration() throws ConfigurationException {
    initializeKeystoreSettings();
    initializeURLs();
    initializeTransportConfigurations();
  }

  public String getListenerURL() {
    return _listenerURL;
  }

  public WebTarget getListenerTarget() {
    return _listenerTarget;
  }

  /**
   * Sets the Listener URL. Checks to see if the URL is valid, or it does
   * nothing.
   * 
   * @param listenerURL new URL for the client listener
   * @return if this actually changed the listener URL
   */
  public boolean setListenerURL(String listenerURL) {
    String newURL = _substitutor.replace(listenerURL);
    boolean changed = false;
    if (!_urlValidator.isValid(newURL)) {
      error("The Response Listener URL specified is invalid. Continuing with previous value.");
    } else {
      if (!newURL.equalsIgnoreCase(_listenerURL)) {
        _listenerURL = newURL;
        _listenerTarget = createListenerTarget(newURL);
        changed = true;
      }
    }
    return changed;
  }

  public String getResponseURL() {
    return _responseURL;
  }

  /**
   * Sets the respondTo URL for the client probes to use. If the URL is invalid
   * it does nothing.
   * 
   * @param responseURL URL for the client probes to use
   */
  public void setResponseURL(String responseURL) {
    String newURL = _substitutor.replace(responseURL);
    if (!_urlValidator.isValid(newURL)) {
      error("The RespondTo URL specified is invalid. Continuing with previous value.");
    } else {
      this._responseURL = newURL;
    }
  }

  public ArrayList<TransportConfig> getTransportConfigs() {
    return _transportConfigs;
  }

  public SSLContextConfigurator getSSLContextConfigurator() {
    return _sslContextConfigurator;
  }

  public String getKeystore() {
    return _keystore;
  }

  public void setKeystore(String _keystore) {
    this._keystore = _keystore;
  }

  public String getKSPassword() {
    return _ksPassword;
  }

  public void setKSPassword(String _kspassword) {
    this._ksPassword = _kspassword;
  }

  public String getTruststore() {
    return _truststore;
  }

  public void setTruststore(String _truststore) {
    this._truststore = _truststore;
  }

  public String getTSPassword() {
    return _tsPassword;
  }

  public boolean isAllowHTTPS() {
    return _allowHTTPS;
  }

  public void setTSPassword(String _tsPassword) {
    this._tsPassword = _tsPassword;
  }

  /**
   * This validation method will check to see if the SSL trust stores are
   * configured correctly. It will adjust the logging level for the
   * SSLContextConfigurator to ensure that the actual errors that occur during
   * validation are shown to the console (as well as any other handlers).
   * 
   * @throws ConfigurationException
   */
  public void validateKeystoreConfiguration() throws ConfigurationException {

    //Logger logger = Grizzly.logger(SSLContextConfigurator.class);
    //Level level = logger.getLevel();

    //Handler consoleHandler = new ConsoleHandler();
    //consoleHandler.setLevel(Level.FINE);
    //logger.addHandler(consoleHandler);

    //logger.setLevel(Level.FINE);
    if (!_sslContextConfigurator.validateConfiguration(true)) {
      // Throwing this ConfigurationException will crash out of the client
      // entirely. The SSL stuff needs to be configured properly for the client
      // to start at all.
      throw new ConfigurationException("The SSL Context is not valid. The SSL client listener WILL NOT WORK PROPERLY.  Check the log and adjust accordingly.");
    } else {
      Console.info("The keystore configuration is valid.");
    }
    //logger.removeHandler(consoleHandler);
    //logger.setLevel(level);
  }

  private void initializeKeystoreSettings() throws ConfigurationException {

    _keystore = _config.getString("keystoreFilename");
    _ksPassword = _config.getString("keystorePassword");
    _truststore = _config.getString("truststoreFilename");
    _tsPassword = _config.getString("truststorePassword");
    
    _sslContextConfigurator = new SSLContextConfigurator();

    if ( StringUtils.isNotEmpty( _keystore ) && StringUtils.isNotEmpty(_ksPassword ) && StringUtils.isNotEmpty( _truststore ) && StringUtils.isNotEmpty( _tsPassword ) ){
        // set up security context contains listener self-signed certificate
        _sslContextConfigurator.setKeyStoreFile(getKeystore());
        _sslContextConfigurator.setKeyStorePass(getKSPassword());
        // contains listener self-signed certificate
        _sslContextConfigurator.setTrustStoreFile(getTruststore());
        _sslContextConfigurator.setTrustStorePass(getTSPassword());

        validateKeystoreConfiguration();
    }else{
    	Console.warn( "***** WARNING: KeyStore and TrustStore were not set.  You will not be able to use SSL/TLS communications.  Update the clientConfig.xml file if SSL/TLS is needed *****" );
    }
  }

  private void initializeURLs() throws ConfigurationException{
    String listenerURL = _config.getString("listenerURL", ResponseListener.DEFAULT_LISTENER_URI.toString()).trim();

    if ( !validSSLSettings( listenerURL, _keystore, _ksPassword )){
        throw new ConfigurationException( "TLS Settings are invalid, Keystore and KeyStore password must be set if using a HTTPS listenerURL, please update clientConfig.xml file." );
    }
    if (!_urlValidator.isValid(listenerURL)) {
      listenerURL = ResponseListener.DEFAULT_LISTENER_URI.toString();
      error("The Response Listener URL specified in the config file is invalid. Continuing with default.");
    }
   
    _listenerURL = listenerURL;
    
    _listenerTarget = createListenerTarget(listenerURL);

    // RespondTo URL

    String respondToURL = _config.getString("respondToURL", listenerURL);

    if (respondToURL.isEmpty()) {
      respondToURL = listenerURL;
      info("The respondTo URL is defaulting to the listenerURL.");
    } else if (!_urlValidator.isValid(respondToURL)) {
      respondToURL = listenerURL;
      error("The respondTo URL specified in the config file is invalid. Continuing with default.");
    }
    _responseURL = respondToURL;
  }

  private boolean validSSLSettings( String listenerURL, String keystore, String password ) {
    if ( StringUtils.startsWithIgnoreCase( listenerURL, "https" ) && ( StringUtils.isEmpty( keystore ) || StringUtils.isEmpty( password ) ) ) {
        return false;
    }
    return true;
}

private WebTarget createListenerTarget(String listenerURL) {
    SSLContext sslContext = _sslContextConfigurator.createSSLContext();
    Client client = ClientBuilder.newBuilder().sslContext(sslContext).hostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
//    Client client = ClientBuilder.newBuilder().build();
   return client.target(listenerURL);
  }

  private void initializeTransportConfigurations() {
    // handle the list of transport information

    // You know, this might be better to do as a JSON (or such) file, but you
    // can't comment out lines in JSON

    _transportConfigs = new ArrayList<TransportConfig>();

    List<HierarchicalConfiguration> transports = _config.configurationsAt("transports.transport");

    for (HierarchicalConfiguration c : transports) {
      String name = c.getString("name");
      boolean enabled = Boolean.parseBoolean(c.getString("enableOnStartup"));
      boolean usesNI = Boolean.parseBoolean(c.getString("usesNI"));
      boolean requiresMC = Boolean.parseBoolean(c.getString("requiresMulticast"));
      String classname = c.getString("classname");
      String configFilename = c.getString("configFilename");

      // if the classname is empty or null then ignore it
      if (classname != null && !classname.isEmpty()) {
        TransportConfig config = new TransportConfig(name);
        config.setClassname(classname);
        config.setEnabled(enabled);
        config.setUsesNetworkInterface(usesNI);
        config.setRequiresMulticast(requiresMC);
        config.setPropertiesFilename(configFilename);

        _transportConfigs.add(config);
      } else {
        warn("Encountered a blank classname in the configuration.  Without a classname, there is no Transport to configure.");
      }
    }

  }

  protected void initializeDefaults() {
    // Nothing to do just yet - if in the future we do decide to have defaults,
    // they will be initialized here.

  }

  @Override
  protected void warn(String string) {
    Console.warn(string);
  }

  @Override
  protected void info(String string) {
    Console.info(string);
  }

  @Override
  protected void error(String string) {
    Console.error(string);
  }

  @Override
  protected void error(String string, Throwable e) {
    Console.error(string);
  }

}
