package it.sad.sii.network;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.Hashtable;

import static org.junit.Assert.*;

/**
 * Created by oskar on 7/5/16.
 */

// TODO: use MockWebServer server for tests instead of real one.
public class RestClientTest extends RestTest {

    @BeforeClass
    static public void setUpOnce() {
        try {
            rest_pu = new RestClient(PU_URL_BASE, PU_USERNAME, PU_PASSWORD, 5000, httpProxy, proxyPort);
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage());
        }
    }

    @Test
    public void testGetEncoding() throws Exception {
        rest_pu.setTimeouts(5000, 5000, 5000);
        rest_pu.disableRetryCircuitBreaker();

        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OFF);

        Hashtable<String, String> parameters = new Hashtable<String, String>();
        parameters.put("limit", "?2");
        parameters.put("placeId", "&685");

        String jsonResponse = rest_pu.get(PU_URL_BASE + "get", parameters);

        assertJsonEquals("GET encoding response different from expected response.", jsonResponse,
                         "{\"args\":{\"limit\":\"?2\",\"placeId\":\"&685\"},\"headers\": " +
                         "{\"Accept\":\"application/json\"," +
                         "\"Accept-Encoding\":\"gzip\",\"Host\":\"httpbin.org\",\"User-Agent\":\"OkHttp " +
                         "RestClient\"}," +
                         "\"origin\":\"2.113.90.244\",\"url\":\"https://httpbin.org/get?limit=%3F2&placeId=%26685\"}");
        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OFF);
    }

    @Test
    public void testGivenParamsGet() throws Exception {
        rest_pu.setTimeouts(5000, 5000, 5000);
        rest_pu.disableRetryCircuitBreaker();

        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OFF);

        RestRequest request = new RestRequest(rest_pu, RestRequest.HTTPVerb.GET, "get", "id=123&name=pippo", null);

        String jsonResponse = request.doRequest().getData();

        assertJsonEquals("GET with encoded params response different from expected response.", jsonResponse,
                         "{\"args\": {\"id\": \"123\", \"name\": \"pippo\"}, \"headers\": {" +
                         "\"Accept\": \"application/json\", \"Accept-Encoding\": \"gzip\", \"Host\": \"httpbin.org\"," +
                         "\"User-Agent\": \"OkHttp RestClient\" }, \"origin\": \"2.113.90.244\"," +
                         "\"url\": \"https://httpbin.org/get?id=123&name=pippo\" }");
        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OFF);
    }

    @Test
    public void testGzip() throws Exception {
        rest_pu.setTimeouts(5000, 5000, 5000);
        rest_pu.disableRetryCircuitBreaker();

        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OFF);

        String jsonResponse = rest_pu.get(PU_URL_BASE + "gzip");

        assertTrue("No gzipped data returned by server", jsonResponse.length() != 0);
        assertJsonEquals("GET Gzipped response different from expected response.", jsonResponse,
                         "{\"gzipped\": true, \"headers\": {\"Accept\": \"application/json\"," +
                         "\"Accept-Encoding\": \"gzip\", \"Host\": \"httpbin.org\", " +
                         "\"User-Agent\": \"OkHttp RestClient\"}, \"method\": \"GET\"," +
                         " \"origin\": \"2.113.90.244\"}");
        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OFF);
    }

    @Test
    public void testPost() throws Exception {
        rest_pu.setTimeouts(5000, 5000, 5000);
        rest_pu.disableRetryCircuitBreaker();

        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OFF);

        int statusCode = rest_pu.post(PU_URL_BASE + "post", "ABCDEDFG");

        assertEquals("POST statuscode not 200", statusCode, 200);
        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OFF);
    }

    @Test
    public void testPostWithResponse() throws Exception {
        rest_pu.setTimeouts(5000, 5000, 5000);
        rest_pu.disableRetryCircuitBreaker();

        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OFF);

        final String postData = "ABCDEDFGHIJ";

        RestResponse response = rest_pu.postResponse(PU_URL_BASE + "post", postData);

        assertEquals("POST statuscode not 200", response.getCode(), 200);
        assertNotNull("POST was null", response);
        assertJsonEquals("POST response differs from expected response.", response.getData(),
                         "{\"args\": {}, \"data\": \"ABCDEDFGHIJ\", \"files\": {}, \"form\": {}, \"headers\": {" +
                         "\"Accept\": \"application/json\", \"Accept-Encoding\": \"gzip\", " +
                         "\"Content-Length\": \"11\", \"Content-Type\": \"application/json; charset=utf-8\"," +
                         "\"Host\": \"httpbin.org\", \"User-Agent\": \"OkHttp RestClient\"  },  \"json\": null," +
                         "\"origin\": \"2.113.90.244\", \"url\": \"https://httpbin.org/post\"}");
        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OFF);
    }

    @Test
    public void testRetryTimeout() throws Exception {
        // set timeout below the response time of the server (2s)
        rest_pu.setTimeouts(1000, 1000, 1000);
        rest_pu.enableRetryCircuitBreaker(3, 1000, 3000);

        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.CLOSED);

        try {
            rest_pu.get(PU_URL_BASE + "/delay/2");
        } catch (UnsupportedOperationException e) {
            assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OPEN);
            return;
        }

        fail("Request should throw UnsupportedOperationException");
    }

    @Test
    public void testRetryTimeoutAndBlockFurtherRequest() throws Exception {
        // set timeout below the response time of the server (2s)
        rest_pu.setTimeouts(1000, 1000, 1000);
        // set circuit breaker to 3000ms so that the following request gets rejected
        rest_pu.enableRetryCircuitBreaker(3, 1000, 3000);

        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.CLOSED);

        // first request fails because of timeouts
        try {
            rest_pu.get(PU_URL_BASE + "/delay/2");
        } catch (UnsupportedOperationException e) {
            assertNotNull(e.getMessage());
            assertEquals("Request should fail", e.getMessage(),
                         "Retrying failed to complete successfully after 3 attempts.");
            assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OPEN);

            // second request fails because CircuitBreaker rejects it
            try {
                rest_pu.get(PU_URL_BASE + "/delay/2");
            } catch (UnsupportedOperationException e2) {
                assertNotNull(e2.getMessage());
                assertTrue("Request should be rejected by the CircuitBreaker",
                           e2.getMessage().startsWith("Requests are not permitted for another"));
                assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OPEN);
            }

            return;
        }

        fail("Request should throw UnsupportedOperationException");
    }

    @Test
    public void testRetryTimeoutAndFurtherRequestWithoutRetry() throws Exception {
        // set timeout below the response time of the server (2s)
        rest_pu.setTimeouts(1000, 1000, 1000);
        // set circuit breaker to 0ms so that the following request does not get rejected
        rest_pu.enableRetryCircuitBreaker(3, 1000, 0);

        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.CLOSED);

        // first request fails because of timeouts
        try {
            rest_pu.get(PU_URL_BASE + "/delay/2");
        } catch (UnsupportedOperationException e) {
            assertNotNull(e.getMessage());
            assertEquals("Request should fail", e.getMessage(),
                         "Retrying failed to complete successfully after 3 attempts.");
            assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OPEN);

            // also second request fails because of timeouts
            try {
                rest_pu.get(PU_URL_BASE + "/delay/2");
            } catch (UnsupportedOperationException e2) {
                assertNotNull(e2.getMessage());
                assertEquals("Request should fail", e.getMessage(),
                             "Retrying failed to complete successfully after 3 attempts.");
                assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.OPEN);
            }

            // third request succeeds because we do not receive a timeout (other URL that returns immediately)
            String response = rest_pu.get(PU_URL_BASE + "/get");
            assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.CLOSED);
            assertJsonEquals("GET response different from expected response.", response,
                             "{\"args\":{},\"headers\":{\"Accept\":\"application/json\",\"Accept-Encoding\":" +
                             "\"gzip\",\"Host\":\"httpbin.org\",\"User-Agent\":\"OkHttp RestClient\"}," +
                             "\"origin\":\"2.113.90.244\",\"url\":\"https://httpbin.org/get\"}");
            LOG.debug(response);
            return;
        }

        fail("Request should throw UnsupportedOperationException");
    }

    @Test
    public void testRetryUnsupported() throws Exception {
        rest_pu.setTimeouts(1000, 1000, 1000);
        rest_pu.enableRetryCircuitBreaker(3, 1000, 3000);

        assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.CLOSED);

        // Should send just one request!
        try {
            rest_pu.get(PU_URL_BASE + "/xxxdelay/2");
        } catch (UnsupportedOperationException e) {
            assertEquals(rest_pu.getRetryCircuitBreakerState(), RestClient.RetryCircuitBreakerState.CLOSED);
            return;
        }

        fail("Request should throw UnsupportedOperationException");
    }
}

