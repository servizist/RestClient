package it.sad.sii.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Created by oskar on 7/13/16.
 */
public class RestTest {

    protected static final Logger LOG = Logger.getLogger(RestTest.class);
    static it.sad.sii.network.RestClient rest_pu;

    protected static final int proxyPort = 3128;
    protected static final String httpProxy = "proxy.servizist.it";

    // Public online test server
    protected static final String PU_URL_BASE = "https://httpbin.org/";
    protected static final String PU_USERNAME = "pinco";
    protected static final String PU_PASSWORD = "1234567890";


    protected void assertJsonEquals(String message, String jsonResponse, String expectedJsonResponse) {
        // remove ip of sending host because it may change
        jsonResponse = jsonResponse.replaceAll(",\\s*\"origin\"\\s*:\\s*\"\\d+\\.\\d+\\.\\d+\\.\\d+\"\\s*", "")
                                   .replaceAll(",\\s*\"origin\"\\s*:\\s*\"\\d+\\.\\d+\\.\\d+\\.\\d+\"\\s*", "");

        JsonParser jparser = new JsonParser();
        JsonElement response = jparser.parse(jsonResponse);
        JsonElement expected = jparser.parse(expectedJsonResponse);
        assertEquals(message, expected, response);
    }

}
