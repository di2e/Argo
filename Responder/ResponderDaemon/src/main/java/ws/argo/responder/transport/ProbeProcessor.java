package ws.argo.responder.transport;

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
