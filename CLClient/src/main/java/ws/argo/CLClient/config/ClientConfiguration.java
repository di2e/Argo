package ws.argo.CLClient.config;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;

import net.dharwin.common.tools.cli.api.console.Console;
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

  private ArrayList<TransportConfig> _transportConfigs;
  private String                     _listenerURL;
  private String                     _responseURL;

  WebTarget                          _listenerTarget;

  public ClientConfiguration() {
    initializeDefaults();
  }

  public ClientConfiguration(String filename) throws ConfigurationException {
    super(filename);
  }

  @Override
  public void initializeConfiguration() {
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
        _listenerTarget = ClientBuilder.newClient().target(listenerURL);
        changed = true;
      }
    }
    return changed;
  }

  public String getResponseURL() {
    return _responseURL;
  }

  /**
   * Sets the respondTo URL for the client probes to use.  If the URL is invalid it does nothing.
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

  private void initializeURLs() {
    String listenerURL = _config.getString("listenerURL", ResponseListener.DEFAULT_LISTENER_URI.toString());

    if (!_urlValidator.isValid(listenerURL)) {
      listenerURL = ResponseListener.DEFAULT_LISTENER_URI.toString();
      error("The Response Listener URL specified in the config file is invalid. Continuing with default.");
    }
    _listenerURL = listenerURL;
    _listenerTarget = ClientBuilder.newClient().target(listenerURL);

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

      if (configFilename != null) {
        TransportConfig config = new TransportConfig(name);
        config.setClassname(classname);
        config.setEnabled(enabled);
        config.setUsesNetworkInterface(usesNI);
        config.setRequiresMulticast(requiresMC);
        config.setPropertiesFilename(configFilename);

        _transportConfigs.add(config);
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
