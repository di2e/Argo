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

package ws.argo.probe;

import java.util.ArrayList;
import java.util.List;

import ws.argo.probe.transport.Transport;

/**
 * The ProbeGenerator is the mechanism that actually sends out the wireline
 * format over UDP on the network. It will take a probe and then send it
 * according to the transport supplied.
 * 
 * <p>
 * The ProbeGenerator takes an Transport instance to do the actual messy work of
 * sending out the probe. See {@linkplain ProbeGeneratorFactory} to see how you
 * can create instances of a ProbeGenerator.
 * 
 * @author jmsimpson
 *
 */
public class ProbeGenerator {

  private Transport probeTransport;

  /**
   * Manually create the ProbeGenerator instance.
   * 
   * @param transport the transport instance the ProbeGenerator should use. See
   *          {@linkplain Transport}.
   * @throws ProbeGeneratorException if something went wrong
   */
  public ProbeGenerator(Transport transport) throws ProbeGeneratorException {
    this.probeTransport = transport;
  }

  /**
   * Send the probe. It will take the provided probe and then disassemble it
   * into a number of smaller probes based on the max payload size of the
   * transport provided.
   * 
   * @param probe the probe to send
   * @return the list of actual probes sent (which were created when the
   *         provided probe was spit into smaller ones that complied with the
   *         max payload size of the transport)
   * @throws ProbeGeneratorException if something goes wrong
   */
  public List<Probe> sendProbe(Probe probe) throws ProbeGeneratorException {

    List<Probe> actualProbesToSend = splitProbe(probe);

    for (Probe probeSegment : actualProbesToSend) {
      probeTransport.sendProbe(probeSegment);
    }

    return actualProbesToSend;

  }

  /**
   * Close the underlying transport if necessary.
   * 
   * @throws ProbeGeneratorException if something goes wrong
   */
  public void close() throws ProbeGeneratorException {
    probeTransport.close();
  }

  /**
   * This method will take the given probe to send and then chop it up into
   * smaller probes that will fit under the maximum packet size specified by the
   * transport. This is important because Argo wants the UDP packets to not get
   * chopped up by the network routers of at all possible. This makes the
   * overall reliability of the protocol a little higher.
   * 
   * @param probe the offered probe instance
   * @return the list of probes the original one was split into (might be the
   *         same as the original probe)
   */
  private List<Probe> splitProbe(Probe probe) {
    // TODO Auto-generated method stub
    List<Probe> actualProbeList = new ArrayList<Probe>();
//    int maxPayloadSize = this.probeTransport.maxPayloadSize();
    
    // use a strategy to split up the probe into biggest possible chunks
    // all respondTo address must be included - if that's a problem then throw an exception - the user will need to do some thinking.
    // start by taking one siid or scid off put it into a temp probe, check the payload size of the probe and repeat until target probe is right size.
    // put the target probe in the list, make the temp probe the target probe and start the process again.
    // Not sure how to do this with wireline compression involved.

    // TODO: Actually split the probe in to smaller chuck is necessary

    actualProbeList.add(probe);

    return actualProbeList;
  }

  /**
   * Return the description of the ProbeGenerator.
   * @return description
   */
  public String getDescription() {
    StringBuffer buf = new StringBuffer();
    buf.append("ProbeGenerator for ");
    buf.append(probeTransport.toString());
    return buf.toString();
  }

}
