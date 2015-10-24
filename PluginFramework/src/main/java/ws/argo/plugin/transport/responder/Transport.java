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

package ws.argo.plugin.transport.responder;

import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.exception.TransportException;

/**
 * The Transport interfaces defines the API for an Argo Responder transport. The
 * idea is that a transport will be run in its own thread - therefore the
 * Runnable declaration.
 * 
 * @author jmsimpson
 *
 */
public interface Transport extends Runnable {

  void initialize(ProbeProcessor p, String propertiesFilename) throws TransportConfigException;

  public void shutdown() throws TransportException;

  public String transportName();

}
