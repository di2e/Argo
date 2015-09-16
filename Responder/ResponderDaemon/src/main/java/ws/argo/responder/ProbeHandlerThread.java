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

package ws.argo.responder;

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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.probe.ProbeWrapper.RespondToURL;
import ws.argo.wireline.response.ResponseWrapper;

/**
 * The ProbeHandlerThread is the worker thread for the {@link Responder}. The
 * Responder will launch a new ProbeHandlerThread with it receives a probe off
 * the wire. It will run through all of the probe handlers and process any
 * positive hits that it gets. It then compiles the results (discovered
 * Services), packages them up in a response and sends the response back to the
 * list of respondTo addresses in the probe. If it fails on one respondTo
 * address, the it will just move to the next (if there is one).
 * 
 * @author jmsimpson
 *
 */
public class ProbeHandlerThread implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(ProbeHandlerThread.class.getName());

  // 5 minutes
  private static final long probeCacheTimeout = 5 * 60 * 1000;

  private static Map<String, Long> handledProbes = new ConcurrentHashMap<String, Long>();

  protected CloseableHttpClient httpClient;

  ArrayList<ProbeHandlerPluginIntf> handlers;
  ProbeWrapper                      probe;
  boolean                           noBrowser;
  Responder                         responder;

  /**
   * Create a new ProbeHandler thread that will process a probe in a
   * multi-threaded way.
   * 
   * @param probe - the actual probe payload
   * @param noBrowser - a flag that indicated whether a naked probe should be
   *          processed
   */
  public ProbeHandlerThread(Responder responder, ProbeWrapper probe, boolean noBrowser) {
    this.responder = responder;
    this.handlers = responder.getHandlers();
    this.probe = probe;
    this.httpClient = responder.httpClient;
    this.noBrowser = noBrowser;
  }

  /**
   * If the probe yields responses from the handler, then send this method will
   * send the responses to the given respondTo addresses.
   * 
   * @param respondToURL - address to send the response to
   * @param payloadType - JSON or XML
   * @param payload - the actual service records to return
   * @return true if the send was successful
   */
  private boolean sendResponse(String respondToURL, String payloadType, ResponseWrapper payload) {

    // This method will likely need some thought and care in the error handling
    // and error reporting
    // It's a had job at the moment.

    String responseStr = null;
    String contentType = null; // MIME type
    boolean success = true;

    switch (payloadType) {
      case "XML": {
        responseStr = payload.toXML();
        contentType = "application/xml";
        break;
      }
      case "JSON": {
        responseStr = payload.toJSON();
        contentType = "application/json";
        break;
      }
      default:
        responseStr = payload.toJSON();
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
          throw new RuntimeException("Failed : HTTP error code : " + httpResponse.getStatusLine().getStatusCode());
        }

        if (statusCode != 204) {
          BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));

          LOGGER.fine("Successful response from response target - " + respondToURL);
          String output;
          LOGGER.fine("Output from Listener .... \n");
          while ((output = br.readLine()) != null) {
            LOGGER.fine(output);
          }
          br.close();
        }
      } finally {
        httpResponse.close();
      }

      LOGGER.info("Successfully handled probeID: " + probe.getProbeId() + " sending response to: " + respondToURL);

    } catch (MalformedURLException e) {
      success = false;
      LOGGER.log(Level.SEVERE, "MalformedURLException occured  for probeID " + payload.getProbeID() + "\nThe respondTo URL was a no good.  respondTo URL is: " + respondToURL);
    } catch (IOException e) {
      success = false;
      LOGGER.log(Level.SEVERE, "An IOException occured for probeID " + payload.getProbeID(), e);
    } catch (Exception e) {
      success = false;
      LOGGER.log(Level.SEVERE, "Some other error occured for probeID " + payload.getProbeID() + ".  respondTo URL is: " + respondToURL, e);
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
      if (delta < probeCacheTimeout) {
        isProbeHandled = true; // yup, I have handled this before. If past
                               // timeout, then it's like I never saw it before
      }
    }

    return isProbeHandled;
  }

  /**
   * Handle the probe.
   */
  public void run() {

    ResponseWrapper response = null;

    LOGGER.info("Received probe id: " + probe.getProbeId());

    // Only handle probes that we haven't handled before
    // The Probe Generator needs to send a stream of identical UDP packets
    // to compensate for UDP reliability issues. Therefore, the Responder
    // will likely get more than 1 identical probe. We should ignore
    // duplicates.
    if (!isProbeHandled(probe.getProbeId())) {

      if (this.noBrowser && probe.isNaked()) {
        LOGGER.warning("Responder set to noBrowser mode. Discarding naked probe with id: " + probe.getProbeId());
      } else {

        for (ProbeHandlerPluginIntf handler : handlers) {
          response = handler.handleProbeEvent(probe);
          if (!response.isEmpty()) {
            LOGGER.fine("Response to probe [" + probe.getProbeId() + "] includes " + response.numberOfServices());
            Iterator<RespondToURL> respondToURLs = probe.getRespondToURLs().iterator();

            if (probe.getRespondToURLs().isEmpty())
              LOGGER.warning("Processed probe [" + probe.getProbeId() + "] with no respondTo address. That's odd.");
            
            if (respondToURLs.hasNext()) {
              RespondToURL respondToURL = respondToURLs.next();
              // we are ignoring the label for now
              boolean success = sendResponse(respondToURL.url, probe.getRespondToPayloadType(), response);
              if (!success) {
                LOGGER.warning("Issue sending probe [" + probe.getProbeId() + "] response to [" + respondToURL.url + "]");
              }
            }

          } else {
            LOGGER.fine("Response is empty.  Not sending empty response.");
          }
        }

      }

      markProbeAsHandled(probe.getProbeId());

      responder.probeProcessed();

    } else {
      LOGGER.info("Discarding duplicate/handled probe with id: " + probe.getProbeId());
    }
  }
}
