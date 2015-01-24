package ws.argo.Responder.plugin;

import java.io.IOException;

import ws.argo.Responder.ProbePayloadBean;
import ws.argo.Responder.ResponsePayloadBean;

public interface ProbeHandlerPluginIntf {

	public ResponsePayloadBean probeEvent(ProbePayloadBean payload);
	public void setPropertiesFilename(String filename) throws IOException;
}
