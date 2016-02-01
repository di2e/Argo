package ws.argo.common.config.test;

import org.apache.commons.configuration.ConfigurationException;

import ws.argo.common.config.ResolvingXMLConfiguration;

public class TestXMLConfiguration extends ResolvingXMLConfiguration {

  public TestXMLConfiguration() {
  }

  public TestXMLConfiguration(String filename) throws ConfigurationException {
    super(filename);
  }
  
  public String getString(String attr) {
    return _config.getString(attr);
  }

  @Override
  protected void initializeConfiguration() {
  }

  @Override
  protected void warn(String string) {
  }

  @Override
  protected void info(String string) {
  }

  @Override
  protected void error(String string) {
  }

  @Override
  protected void error(String string, Throwable e) {
  }

}
