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

package ws.argo.plugin.probehandler;

import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.response.ResponseWrapper;

/**
 * This is the interface for a probe handler. It's not a complicated animal.
 * When a probe handler is instantiated it should be initialized with the
 * initializeWithPropertiesFilename(String filename). This call will do whatever
 * needs to be done for initialization included launching other threads
 * associated with the handler.
 * 
 * @author jmsimpson
 *
 */
public interface ProbeHandlerPlugin {

  public ResponseWrapper handleProbeEvent(ProbeWrapper payload);

  public void initializeWithPropertiesFilename(String filename) throws ProbeHandlerConfigException;
  
  public String pluginName();
}
