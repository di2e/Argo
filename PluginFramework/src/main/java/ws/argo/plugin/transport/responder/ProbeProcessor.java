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

import ws.argo.wireline.probe.ProbeWrapper;

/**
 * The ProbeProcessor interface defines the API a Transport needs to call in
 * order to process the probe that just came in over the wire. I guess you could
 * call it a callback.
 * 
 * @author jmsimpson
 *
 */
public interface ProbeProcessor {

  public void processProbe(ProbeWrapper probe);
  
  public float probesPerSecond();
  
  public int probesProcessed();
  
  public void probeProcessed();
  
  public String getRuntimeID();
}
