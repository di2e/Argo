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

package ws.argo.responder.configuration;

/**
 * Utility class to encapsulate the configuration of the plugin information. The
 * plugins include the app handler and the transports.
 * 
 * @author jmsimpson
 *
 */
public class PluginConfig {

  public String classname;
  public String configFilename;

  public PluginConfig(String classname, String configFilename) {
    this.classname = classname;
    this.configFilename = configFilename;
  }

  public String getClassname() {
    return classname;
  }

  public void setClassname(String classname) {
    this.classname = classname;
  }

  public String getConfigFilename() {
    return configFilename;
  }

  public void setConfigFilename(String configFilename) {
    this.configFilename = configFilename;
  }

}
