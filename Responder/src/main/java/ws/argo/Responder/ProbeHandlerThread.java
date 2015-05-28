package ws.argo.Responder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import ws.argo.ArgoWirelineFormat.probe.xml.Probe.Ra.RespondTo;


public class ProbeHandlerThread extends Thread {

	private final static Logger LOGGER = Logger.getLogger(Responder.class.getName());
	
	private final static long probeCacheTimeout = 5*60*1000; // 5 minutes

	private static Map<String, Long> handledProbes = new ConcurrentHashMap<String, Long>();

	
	protected CloseableHttpClient httpClient;
	
	ArrayList<ProbeHandlerPluginIntf> handlers;
	String probeStr;
	boolean noBrowser;
	
	public ProbeHandlerThread(ArrayList<ProbeHandlerPluginIntf> handlers, String probeStr, CloseableHttpClient httpClient, boolean noBrowser) {
		this.handlers = handlers;
		this.probeStr = probeStr;
		this.httpClient = httpClient;
		this.noBrowser = noBrowser;
	}

	private ProbePayloadBean parseProbePayload(String payload) throws JAXBException {
		
		ProbePayloadBean probePayload = new ProbePayloadBean(payload);
		
		return probePayload;
	}

	private boolean sendResponse(String respondToURL, String payloadType, ResponsePayloadBean payload) {
		
		// This method will likely need some thought and care in the error handling and error reporting
		// It's a had job at the moment.
		
		String responseStr = null;
		String contentType = null;  //MIME type
		boolean success = true;
		
		switch (payloadType) {
			case "XML" : {
				responseStr = payload.toXML();
				contentType = "application/xml";
				break;
			}
			case "JSON" : {
				responseStr = payload.toJSON();
				contentType = "application/json";
				break;
			}
			default: responseStr = payload.toJSON();
		}
	
		try {
			
			HttpPost postRequest = new HttpPost(respondToURL);

			StringEntity input = new StringEntity(responseStr);
			input.setContentType(contentType);
			postRequest.setEntity(input);

			LOGGER.fine("Sending response");
			LOGGER.fine("Response payload:");
			LOGGER.fine(responseStr);
			CloseableHttpResponse httpResponse = httpClient.execute(postRequest);
			try {
		
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				if (statusCode > 300) {
					throw new RuntimeException("Failed : HTTP error code : "
							+ httpResponse.getStatusLine().getStatusCode());
				}
	
				if (statusCode != 204) {
					BufferedReader br = new BufferedReader(new InputStreamReader(
						(httpResponse.getEntity().getContent())));
	
					LOGGER.fine("Successful response from response target - "+respondToURL);
					String output;
					LOGGER.fine("Output from Listener .... \n");
					while ((output = br.readLine()) != null) {
						LOGGER.fine(output);
					}
				}
			} finally {
				httpResponse.close();
			}
			
			LOGGER.info("Successfully handled probeID: "+payload.probeID+" sending response to: "+respondToURL);

		} catch (MalformedURLException e) {
			success = false;
			LOGGER.log(Level.SEVERE, "MalformedURLException occured  for probeID "+payload.getProbeID()+"\nThe respondTo URL was a no good.  respondTo URL is: "+respondToURL);
		} catch (IOException e) {
			success = false;
			LOGGER.log(Level.SEVERE,"An IOException occured for probeID "+payload.getProbeID(), e);
		} catch (Exception e) {
			success = false;
			LOGGER.log(Level.SEVERE, "Some other error occured for probeID "+payload.getProbeID()+".  respondTo URL is: "+respondToURL, e);
		}
		
		return success;
		
	}
	
	private void markProbeAsHandled(String probeID) {
		handledProbes.put(probeID, Long.valueOf(System.currentTimeMillis()));
	}

	private boolean isProbeHandled(String probeID) {
		
		boolean isProbeHandled = false;
		long now = System.currentTimeMillis();
		
		Long lastTime = handledProbes.get(probeID);
		
		if (lastTime != null) {
			long delta = now - lastTime.longValue();
			if (delta < probeCacheTimeout)
				isProbeHandled = true;  // yup, I have handled this before.  If past timeout, then it's like I never saw it before
		}
		
		return isProbeHandled;
	}

	
	public void run() {
		
		ResponsePayloadBean response = null;

		try {
			ProbePayloadBean payload = parseProbePayload(probeStr);
			
			LOGGER.info("Received probe id: "+payload.probe.getId());
			
			
			// Only handle probes that we haven't handled before
			// The Probe Generator needs to send a stream of identical UDP packets
			// to compensate for UDP reliability issues.  Therefore, the Responder
			// will likely get more than 1 identical probe.  We should ignore duplicates.
			if (!isProbeHandled(payload.probe.getId())) {	
				
				if (this.noBrowser & payload.isNaked()) {
					LOGGER.warning("Responder set to noBrowser mode. Discarding naked probe with id: "+payload.probe.getId());
				} else {
					
					for (ProbeHandlerPluginIntf handler : handlers) {
						response = handler.probeEvent(payload);
						if (!response.isEmpty()) {
							LOGGER.fine("Response includes "+response.numberOfServices());
							Iterator<RespondTo> respondToURLs = payload.probe.getRa().getRespondTo().iterator();
							boolean processRespondToURL = true;
							RespondTo respondToURL = respondToURLs.next();
							while (processRespondToURL) {
								// we are ignoring the label for now
								boolean success = sendResponse(respondToURL.getValue(), payload.probe.getRespondToPayloadType(), response);
								processRespondToURL = !success;
							}
							
						} else {
							LOGGER.fine("Response is empty.  Not sending empty response.");
						}
					}
					
				}

				markProbeAsHandled(payload.probe.getId());
				
			} else {
				LOGGER.info("Discarding duplicate/handled probe with id: "+payload.probe.getId());
			}

		} catch (JAXBException e) {
			LOGGER.log(Level.SEVERE, "Error occured while parsing probe: ", e);
		}
	}
}
