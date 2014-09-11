package net.di2e.rtsd.Responder.plugin;

import java.io.IOException;

import net.di2e.rtsd.Responder.ProbePayloadBean;
import net.di2e.rtsd.Responder.ResponsePayloadBean;

public interface ProbeHandlerPluginIntf {

	public ResponsePayloadBean probeEvent(ProbePayloadBean payload);
	public void setPropertiesFilename(String filename) throws IOException;
}
