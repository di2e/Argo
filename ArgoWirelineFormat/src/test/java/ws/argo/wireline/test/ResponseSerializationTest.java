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

package ws.argo.wireline.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.argo.wireline.response.JSONSerializer;
import ws.argo.wireline.response.ResponseParseException;
import ws.argo.wireline.response.ResponseWrapper;
import ws.argo.wireline.response.ServiceWrapper;
import ws.argo.wireline.response.XMLSerializer;

/**
 * Test the response wireline payloads.
 * 
 * @author jmsimpson
 *
 */
public class ResponseSerializationTest {

  private static final String TESTING_RESPONSE_ID = "urn:uuid:9c6bd8aa-bf9b-45aa-9348-24897d89e86f";
  private static String       testResponseXMLPayload;
  private static String       testResponseXMLPayloadMultipleAccessPoints;
  private static String       testResponseXMLPayloadMultipleServices;
  private static String       testResponseJSONPayload;
  private static String       testResponseJSONPayloadMultipleAccessPoints;
  private static String       testResponseJSONPayloadMultipleServices;
  private static String       corruptPayload1;

  @BeforeClass
  public static void setupProbeSender() throws IOException {
    System.out.println("Java version: " + System.getProperty("java.version"));
    readXMLFiles();
  }

  private static void readXMLFiles() throws IOException {

    // XML Response Formats

    // Read the completely filled out probe test file for comparison
    assertNotNull("testResponsePayload.xml file missing", ProbeSerializationTest.class.getResource("/testResponsePayload.xml"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/testResponsePayload.xml")) {
      testResponseXMLPayload = IOUtils.toString(is, "UTF-8");
    }

    // Read the completely filled out probe test file for comparison
    assertNotNull("testResponsePayloadMultipleAccessPoints.xml file missing", ProbeSerializationTest.class.getResource("/testResponsePayloadMultipleAccessPoints.xml"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/testResponsePayloadMultipleAccessPoints.xml")) {
      testResponseXMLPayloadMultipleAccessPoints = IOUtils.toString(is, "UTF-8");
    }

    // Read the completely filled out probe test file for comparison
    assertNotNull("testResponsePayloadMultipleServices.xml file missing", ProbeSerializationTest.class.getResource("/testResponsePayloadMultipleServices.xml"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/testResponsePayloadMultipleServices.xml")) {
      testResponseXMLPayloadMultipleServices = IOUtils.toString(is, "UTF-8");
    }

    // JSON Response Formats

    // Read the completely filled out probe test file for comparison
    assertNotNull("testResponsePayload.json file missing", ProbeSerializationTest.class.getResource("/testResponsePayload.json"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/testResponsePayload.json")) {
      testResponseJSONPayload = IOUtils.toString(is, "UTF-8");
    }

    // Read the completely filled out probe test file for comparison
    assertNotNull("testResponsePayloadMultipleAccessPoints.json file missing", ProbeSerializationTest.class.getResource("/testResponsePayloadMultipleAccessPoints.json"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/testResponsePayloadMultipleAccessPoints.json")) {
      testResponseJSONPayloadMultipleAccessPoints = IOUtils.toString(is, "UTF-8");
    }

    // Read the completely filled out probe test file for comparison
    assertNotNull("testResponsePayloadMultipleServices.json file missing", ProbeSerializationTest.class.getResource("/testResponsePayloadMultipleServices.json"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/testResponsePayloadMultipleServices.json")) {
      testResponseJSONPayloadMultipleServices = IOUtils.toString(is, "UTF-8");
    }

    // Read the naked (minimally) filled out probe test file for comparison
    assertNotNull("corruptPayload1.txt file missing", ProbeSerializationTest.class.getResource("/corruptPayload1.txt"));
    try (InputStream is = ProbeSerializationTest.class.getResourceAsStream("/corruptPayload1.txt")) {
      corruptPayload1 = IOUtils.toString(is, "UTF-8");
    }
  }

  private ResponseWrapper createBasicResponseWrapper() {
    ResponseWrapper response = new ResponseWrapper("--PROBE ID--");

    ServiceWrapper service = new ServiceWrapper("--SERVICE ID--");
    service.setConsumability(ServiceWrapper.MACHINE_CONSUMABLE);
    service.setServiceName("Some Service Name");
    service.setDescription("some service description");
    service.setServiceContractID("--CONTRACT ID--");
    service.setContractDescription("some contract description");
    service.setTtl("0");

    service.addAccessPoint("internal", "some ip", "some port", "some url", "basic 1", "some data");

    response.addResponse(service);

    // you would never really do this in production, just testing
    // we need to do this to set the right urn to match the test payload
    response.setResponseID(TESTING_RESPONSE_ID);
    return response;
  }

  private ResponseWrapper createResponseWrapperWithMultipleAccessPoints() {
    ResponseWrapper response = new ResponseWrapper("--PROBE ID--");

    ServiceWrapper service = new ServiceWrapper("--SERVICE ID--");
    service.setConsumability(ServiceWrapper.MACHINE_CONSUMABLE);
    service.setServiceName("Some Service Name");
    service.setDescription("some service description");
    service.setServiceContractID("--CONTRACT ID--");
    service.setContractDescription("some contract description");

    service.addAccessPoint("internal", "some ip", "some port", "some url", "basic 1", "some data");
    service.addAccessPoint("external", "some ip 2", "some port 2", "some url 2", "basic 3", "some data 2");

    response.addResponse(service);

    // you would never really do this in production, just testing
    // we need to do this to set the right urn to match the test payload
    response.setResponseID(TESTING_RESPONSE_ID);
    return response;
  }

  private ResponseWrapper createResponseWrapperWithMultipleServices() {
    ResponseWrapper response = new ResponseWrapper("--PROBE ID--");

    ServiceWrapper service = new ServiceWrapper("--SERVICE ID--");
    service.setConsumability(ServiceWrapper.MACHINE_CONSUMABLE);
    service.setServiceName("Some Service Name");
    service.setDescription("some service description");
    service.setServiceContractID("--CONTRACT ID--");
    service.setContractDescription("some contract description");

    service.addAccessPoint("internal", "some ip", "some port", "some url", "basic 1", "some data");

    response.addResponse(service);

    service = new ServiceWrapper("--SERVICE ID 2--");
    service.setConsumability(ServiceWrapper.MACHINE_CONSUMABLE);
    service.setServiceName("Some Service Name 2");
    service.setDescription("some service description 2");
    service.setServiceContractID("--CONTRACT ID 2--");
    service.setContractDescription("some contract description 2");

    service.addAccessPoint("internal", "some ip", "some port", "some url", "basic 1", "some data");
    service.addAccessPoint("external", "some ip 2", "some port 2", "some url 2", "basic 3", "some data 2");

    response.addResponse(service);

    // you would never really do this in production, just testing
    // we need to do this to set the right urn to match the test payload
    response.setResponseID(TESTING_RESPONSE_ID);
    return response;
  }

  @Test
  public void testMarshallingXMLResponse() throws ResponseParseException {
    ResponseWrapper response = createBasicResponseWrapper();

    XMLSerializer serializer = new XMLSerializer();

    String payload = serializer.marshal(response);

    ResponseWrapper parsedResponse = serializer.unmarshal(payload);
    ResponseWrapper knownGoodResponse = serializer.unmarshal(testResponseXMLPayload);

    assertTrue(parsedResponse.equals(knownGoodResponse));

  }

  @Test
  public void testMarshallingJSONResponse() throws ResponseParseException {
    ResponseWrapper response = createBasicResponseWrapper();

    JSONSerializer serializer = new JSONSerializer();

    String payload = serializer.marshal(response);

    ResponseWrapper parsedResponse = serializer.unmarshal(payload);
    ResponseWrapper knownGoodResponse = serializer.unmarshal(testResponseJSONPayload);

    assertTrue(parsedResponse.equals(knownGoodResponse));
  }

  @Test
  public void testMarshallingXMLResponseWithMultipleAccessPoints() throws ResponseParseException {
    ResponseWrapper response = createResponseWrapperWithMultipleAccessPoints();

    XMLSerializer serializer = new XMLSerializer();

    String payload = serializer.marshal(response);

    ResponseWrapper parsedResponse = serializer.unmarshal(payload);
    ResponseWrapper knownGoodResponse = serializer.unmarshal(testResponseXMLPayloadMultipleAccessPoints);

    assertTrue(parsedResponse.equals(knownGoodResponse));

  }

  @Test
  public void testMarshallingJSONResponseWithMultipleAccessPoints() throws ResponseParseException {
    ResponseWrapper response = createResponseWrapperWithMultipleAccessPoints();

    JSONSerializer serializer = new JSONSerializer();

    String payload = serializer.marshal(response);

    ResponseWrapper parsedResponse = serializer.unmarshal(payload);
    ResponseWrapper knownGoodResponse = serializer.unmarshal(testResponseJSONPayloadMultipleAccessPoints);

    assertTrue(parsedResponse.equals(knownGoodResponse));

  }

  @Test
  public void testMarshallingXMLResponseWithMultipleServices() throws ResponseParseException {
    ResponseWrapper response = createResponseWrapperWithMultipleServices();

    XMLSerializer serializer = new XMLSerializer();

    String payload = serializer.marshal(response);

    ResponseWrapper parsedResponse = serializer.unmarshal(payload);
    ResponseWrapper knownGoodResponse = serializer.unmarshal(testResponseXMLPayloadMultipleServices);

    assertTrue(parsedResponse.equals(knownGoodResponse));

  }

  @Test
  public void testMarshallingJSONResponseWithMultipleServices() throws IOException, ResponseParseException {
    ResponseWrapper response = createResponseWrapperWithMultipleServices();

    JSONSerializer serializer = new JSONSerializer();

    String payload = serializer.marshal(response);

    ResponseWrapper parsedResponse = serializer.unmarshal(payload);
    ResponseWrapper knownGoodResponse = serializer.unmarshal(testResponseJSONPayloadMultipleServices);

    assertTrue(parsedResponse.equals(knownGoodResponse));

  }

  @Test
  public void testUnmarshallingXMLResponse() throws ResponseParseException {
    XMLSerializer serializer = new XMLSerializer();

    ResponseWrapper response = serializer.unmarshal(testResponseXMLPayload);

    assertTrue(response.numberOfServices() == 1);
    assertTrue(TESTING_RESPONSE_ID.equals(response.getResponseID()));
  }

  @Test
  public void testUnmarshallingJSONResponse() throws ResponseParseException {
    JSONSerializer serializer = new JSONSerializer();

    ResponseWrapper response = serializer.unmarshal(testResponseJSONPayload);

    assertTrue(response.numberOfServices() == 1);
    assertTrue(TESTING_RESPONSE_ID.equals(response.getResponseID()));

  }

  @Test(expected = ResponseParseException.class)
  public void testUnmarshallingCorruptXMLResponse() throws ResponseParseException {
    XMLSerializer serializer = new XMLSerializer();

    serializer.unmarshal(corruptPayload1);

  }

  @Test(expected = ResponseParseException.class)
  public void testUnmarshallingCorruptJSONResponse() throws ResponseParseException {
    XMLSerializer serializer = new XMLSerializer();

    serializer.unmarshal(corruptPayload1);

  }

}
