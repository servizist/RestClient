package it.sad.sii.network;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Hashtable;


/**
 * Created by ldematte on 7/2/14.
 */
public class RestRequest {

    public RestResponse doRequest() throws IOException, URISyntaxException {
        return restClient.doRequest(this);
    }

    public enum HTTPVerb {
        GET,
        POST,
        PUT,
        DELETE
    }

    private final RestClient restClient;
    private final HTTPVerb verb;
    private final String action;
    private final Hashtable<String, String> params;
    private final String encodedParams;
    private final String content;

    public RestRequest(RestClient restClient, HTTPVerb verb, String action, Hashtable<String, String> params) {
        this.restClient = restClient;
        this.verb = verb;
        this.action = action;
        this.params = params;
        this.encodedParams = null;
        this.content = null;
    }


    public RestRequest(RestClient restClient, HTTPVerb verb, String action, String params, String content) {
        this.restClient = restClient;
        this.verb = verb;
        this.action = action;
        this.encodedParams = params;
        this.params = null;
        this.content = content;
    }

    public RestRequest(RestClient restClient, HTTPVerb verb, String action) {
        this.restClient = restClient;
        this.verb = verb;
        this.action = action;
        this.params = null;
        this.encodedParams = null;
        this.content = null;
    }

    public RestRequest(RestClient restClient, HTTPVerb verb, String action, String content) {
        this.restClient = restClient;
        this.verb = verb;
        this.action = action;
        this.params = null;
        this.encodedParams = null;
        this.content = content;
    }

    public RestRequest(RestClient restClient, HTTPVerb verb, String action, Hashtable<String, String> params,
                       String content) {
        this.restClient = restClient;
        this.verb = verb;
        this.action = action;
        this.params = params;
        this.encodedParams = null;
        this.content = content;
    }

    public HTTPVerb getVerb() {
        return verb;
    }

    public String getAction() {
        return action;
    }

    public Hashtable<String, String> getParams() {
        return params;
    }

    public String getEncodedParams() {
        return encodedParams;
    }

    public String getContent() {
        return content;
    }
}
