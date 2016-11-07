package it.sad.sii.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;


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
    private final Map<String, String> params;
    private final String content;
    private final Map<String, String> headers;

    public RestRequest(RestClient restClient, HTTPVerb verb, String action, Map<String, String> params,
                       String content, Map<String, String> headers) {
        this.restClient = restClient;
        this.verb = verb;
        this.action = action;
        this.params = params;
        this.content = content;
        this.headers = headers;
    }

    public RestRequest(RestClient restClient, HTTPVerb verb, String action, Map<String, String> params) {
        this(restClient, verb, action, params, null, Collections.<String, String>emptyMap());
    }

    public RestRequest(RestClient restClient, HTTPVerb verb, String action, String params, String content)
            throws UnsupportedEncodingException {
        this(restClient, verb, action, UrlUtils.splitQuerySingle(params), content,
             Collections.<String, String>emptyMap());
    }

    public RestRequest(RestClient restClient, HTTPVerb verb, String action) {
        this(restClient, verb, action, Collections.<String, String>emptyMap(), null,
             Collections.<String, String>emptyMap());
    }

    public RestRequest(RestClient restClient, HTTPVerb verb, String action, String content) {
        this(restClient, verb, action, Collections.<String, String>emptyMap(), content,
             Collections.<String, String>emptyMap());
    }

    public HTTPVerb getVerb() {
        return verb;
    }

    public String getAction() {
        return action;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getContent() {
        return content;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
