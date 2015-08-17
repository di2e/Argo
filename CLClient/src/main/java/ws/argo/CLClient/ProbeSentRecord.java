package ws.argo.CLClient;

import java.util.Date;

import ws.argo.probe.Probe;

/**
 * This class encapsulates a record of an attempt to send a probe. It contains a
 * copy of the probe that was actually sent, the date-time it was sent and the
 * results reported when it was sent.
 * 
 * @author jmsimpson
 *
 */
public class ProbeSentRecord {

  private Date   _sentDate = new Date();
  private Probe  _probe;
  private String _result;

  public ProbeSentRecord(Probe probe, String result) {
    _probe = probe;
    _result = result;
  }

  public Date getSentDate() {
    return _sentDate;
  }

  public Probe getProbe() {
    return _probe;
  }

  public String getResult() {
    return _result;
  }

}
