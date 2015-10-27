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

package ws.argo.responder.test;

import static org.junit.Assert.assertNull;

import org.junit.Test;

import ws.argo.responder.Responder;
import ws.argo.responder.configuration.ResponderConfigException;

/**
 * Lauches the Responder in various guises and see if it works.
 * 
 * @author jmsimpson
 *
 */
public class ResponderLaunchTest {

  protected Responder responder;

  @Test
  public void testResponderHelp() throws ResponderConfigException {

    String[] args = new String[1];
    args[0] = "-h"; // help
    Responder responder = Responder.initialize(args);

    assertNull(responder);

  }

  @Test
  public void testResponderBadArg() throws ResponderConfigException {

    String[] args = new String[1];
    args[0] = "-xx"; // help
    Responder responder = Responder.initialize(args);
    assertNull(responder);

  }

  @Test
  public void testResponderNormalLaunch() throws InterruptedException {

    String configFileProp = System.getProperty("configFile");
    System.out.println("****** Testing configFile = " + configFileProp);
    // String configFileProp = "/config/responderConfig.prop";

    final String[] args = { "-pf", configFileProp };

    Thread responderThread = new Thread("Launch Test Responder") {
      public void run() {
        try {
          responder = Responder.initialize(args);
          org.junit.Assert.assertNotNull(responder);
          responder.run();
        } catch (ResponderConfigException e) {
          org.junit.Assert.fail(e.getLocalizedMessage());
        }
      }
    };
    responderThread.start();

    Thread.sleep(1000);

    responder.stopResponder();

  }

  @Test
  public void testResponderWithOneBadConfigClassname() {

    // how to test this?
  }

  @Test(expected = ResponderConfigException.class)
  public void testResponderLaunchWithNonexistentConfigFile() throws ResponderConfigException {

    String nonexistentConfigFile = "nonexistentConfigFile.prop";
    final String[] args = { "-pf", nonexistentConfigFile };

    Responder responder = Responder.initialize(args);
    assertNull(responder);

  }

}
