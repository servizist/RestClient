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
    static it.sad.sii.network.RestClient rest_rfi;
    static it.sad.sii.network.RestClient rest_ts;
    static it.sad.sii.network.RestClient rest_pu;

    protected static final int proxyPort = 3128;
    protected static final String httpProxy = "proxy.sad.it";

    // Transit server
    protected static final String TS_URL_BASE = "https://mocobus.sii.bz.it/transit-service/";
    protected static final String TS_USERNAME = "";
    protected static final String TS_PASSWORD = "";

    // Public online test server
    protected static final String PU_URL_BASE = "https://httpbin.org/";
    protected static final String PU_USERNAME = "pinco";
    protected static final String PU_PASSWORD = "1234567890";


    protected void assertJsonEquals(String message, String jsonResponse, String expectedJsonResponse) {
        JsonParser jparser = new JsonParser();
        JsonElement response = jparser.parse(jsonResponse);
        JsonElement expected = jparser.parse(expectedJsonResponse);
        assertEquals("JSON response differs from expected response.", response, expected);
    }

}
