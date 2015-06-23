package ws.argo.responder.test;

import java.io.IOException;

import org.junit.Test;

import ws.argo.responder.Responder;
import ws.argo.responder.ResponderConfigException;
import ws.argo.responder.ResponderOperationException;

public class ResponderLaunchTest {

  @Test
  public void testResponderHelp() throws ResponderConfigException, ResponderOperationException {

    String[] args = new String[1];
    args[0] = "-h"; // help
    Responder.main(args);

  }

  @Test
  public void testResponderBadArg() throws ResponderConfigException, ResponderOperationException {

    String[] args = new String[1];
    args[0] = "-xx"; // help
    Responder.main(args);

  }

  @Test
  public void testResponderNormalLaunch() throws InterruptedException {

    // String configFileProp = System.getProperty("configFile");
    // System.out.println("****** Testing configFile = " + configFileProp);
    String configFileProp = "/config/responderConfig.prop";

    final String[] args = { "-pf", configFileProp };

    Thread responderThread = new Thread("Normal Responder") {
      public void run() {
        try {
          Responder.main(args);
        } catch (ResponderConfigException e) {
          org.junit.Assert.fail(e.getLocalizedMessage());
        } catch (ResponderOperationException e) {
          org.junit.Assert.fail(e.getLocalizedMessage());
        }
      }
    };
    responderThread.start();
    
    Thread.sleep(1000);
    
    Responder.stopResponder();
     
  }
  
  @Test
  public void testResponderWithOneBadConfigClassname() {
    
    //how to test this?
  }

  @Test(expected = ResponderConfigException.class)
  public void testResponderLaunchWithNonexistentConfigFile() throws ResponderConfigException, ResponderOperationException {

    String nonexistentConfigFile = "nonexistentConfigFile.prop";
    final String[] args = { "-pf", nonexistentConfigFile };

    Responder.main(args);

  }

}
