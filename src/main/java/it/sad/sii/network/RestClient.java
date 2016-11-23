package it.sad.sii.network;

import com.github.rholder.retry.*;
import com.google.common.base.Predicate;
import okhttp3.*;
import okhttp3.internal.tls.OkHostnameVerifier;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static it.sad.sii.network.RestRequest.HTTPVerb.GET;
import static it.sad.sii.network.RestRequest.HTTPVerb.POST;

/**
 * Class that handles HTTP(S) GET and POST requests.
 * <p/>
 * It has two modes: simple and smart.
 * <p/>
 * 1) Simple Mode: Make each request exactly once (retryCircuitBreakerState = OFF).
 * Is the default mode and can be set with {@link #disableRetryCircuitBreaker()}
 * <p/>
 * 2) Smart Mode: Retry each request 'retries' times with an exponential backoff up until 'maxRetryTime' ms is reached
 * (retryCircuitBreakerState = CLOSED).
 * If the request does not succeed it blocks all following requests for 'maxCircuitBreakerOpenTime' ms
 * (retryCircuitBreakerState = OPEN).
 * After that the next request will be issued in the Simple Mode.
 * Only if that succeeds we re-enter the Smart Mode (retryCircuitBreakerState = CLOSED).
 * This mode can be set with {@link #enableRetryCircuitBreaker(int, int, int)}
 * <p/>
 * Furthermore, we can set the read, write and connect timeouts with {@link #setTimeouts(int, int, int)}
 */
public class RestClient {

    protected final URI serverUri;
    protected final int timeout;
    private final String username;
    private final String password;
    private final KeyStore truststore;
    private Proxy proxy = Proxy.NO_PROXY;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient okHttpClient;

    // Retry and circuit breaker attributes
    // If retryCircuitBreakerState == OFF the values of retries, maxRetryTime and maxCircuitBreakerOpenTime are ignored
    private int retries = 0;
    private int maxRetryTime = 0;
    private int maxCircuitBreakerOpenTime = 3000;
    private long circuitBreakerOpenSince = 0;
    private RetryCircuitBreakerState retryCircuitBreakerState;

    enum RetryCircuitBreakerState {
        OFF,
        CLOSED,
        OPEN
    }

    public RestClient(String serverUrl)
            throws URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this.serverUri = new URI(serverUrl);
        this.timeout = 2000;
        this.truststore = null;
        this.username = null;
        this.password = null;
        disableRetryCircuitBreaker();
        createClient();
    }

    public RestClient(String serverUrl, String username, String password, int timeout)
            throws URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this.username = username;
        this.password = password;
        this.truststore = null;
        this.serverUri = new URI(serverUrl);
        this.timeout = timeout;
        disableRetryCircuitBreaker();
        createClient();
    }

    public RestClient(String serverUrl, String username, String password, int timeout, KeyStore truststore)
            throws URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this.username = username;
        this.password = password;
        this.truststore = truststore;
        this.serverUri = new URI(serverUrl);
        this.timeout = timeout;
        disableRetryCircuitBreaker();
        createClient();
    }

    public RestClient(String serverUrl, String username, String password, int timeout, String httpProxy, int proxyPort)
            throws URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this.username = username;
        this.password = password;
        this.serverUri = new URI(serverUrl);
        this.timeout = timeout;
        this.truststore = null;

        if (httpProxy != null && proxyPort != 0) {
            this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxy, proxyPort));
        } else {
            this.proxy = Proxy.NO_PROXY;
        }
        disableRetryCircuitBreaker();
        createClient();
    }

    /**
     * Sets the timeout in milliseconds for reading, writing and the establishment of the connection
     *
     * @param readTimeout    read timeout
     * @param writeTimeout   write timeout
     * @param connectTimeout connect timeout
     */
    public void setTimeouts(int readTimeout, int writeTimeout, int connectTimeout) {
        okHttpClient = okHttpClient.newBuilder()
                                   .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                                   .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                                   .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                                   .build();
    }

    /**
     * Enables retry and circuit breaker.
     * retries = 0 -> no retries, no circuit breaker -> use disableRetryCircuitBreaker (this function throws an
     * exception)
     *
     * @param retries                   Max number of retries after that CircuitBreaker goes in OPEN state (>0)
     * @param maxRetryTime              Max time in milliseconds between retries (>=0).
     * @param maxCircuitBreakerOpenTime Max time in ms the CircuitBreaker may stay in OPEN state (>=0).
     * @throws IllegalArgumentException if retries <= 0, maxRetryTime < 0, maxCircuitBreakerOpenTime < 0
     */
    public void enableRetryCircuitBreaker(int retries, int maxRetryTime, int maxCircuitBreakerOpenTime) {
        if (retries <= 0)
            throw new IllegalArgumentException("The number of retries has to be greater than zero");
        if (maxRetryTime < 0)
            throw new IllegalArgumentException("The maximum retry time has to be greater than or equal to zero");
        if (maxCircuitBreakerOpenTime < 0)
            throw new IllegalArgumentException(
                    "The maximum circuit breaker open time has to be greater than or equal to zero");

        retryCircuitBreakerState = RetryCircuitBreakerState.CLOSED;

        this.retries = retries;
        this.maxRetryTime = maxRetryTime;
        this.maxCircuitBreakerOpenTime = maxCircuitBreakerOpenTime;
    }

    /**
     * Disables retry and circuit breaker.
     */
    public void disableRetryCircuitBreaker() {
        retryCircuitBreakerState = RetryCircuitBreakerState.OFF;
    }

    public int getRetries() {
        return retries;
    }

    public int getMaxRetryTime() {
        return maxRetryTime;
    }

    public RetryCircuitBreakerState getRetryCircuitBreakerState() {
        return retryCircuitBreakerState;
    }


    private void closeCircuitBreaker() {
        if (retryCircuitBreakerState == RetryCircuitBreakerState.OFF)
            throw new IllegalArgumentException("State cannot be changed from OFF to CLOSED");
        retryCircuitBreakerState = RetryCircuitBreakerState.CLOSED;
    }

    private void openCircuitBreaker() {
        if (retryCircuitBreakerState == RetryCircuitBreakerState.OFF)
            throw new IllegalArgumentException("State cannot be changed from OFF to OPEN");
        circuitBreakerOpenSince = System.currentTimeMillis();
        retryCircuitBreakerState = RetryCircuitBreakerState.OPEN;
    }

    private void createClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        final OkHttpClient.Builder okHttpClientBuilder =
                new OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.MILLISECONDS)
                                          .readTimeout(timeout, TimeUnit.MILLISECONDS)
                                          .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                                          .proxy(proxy);

        // init truststore we need for servers without a valid certificate
        if (truststore != null) {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);

            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            okHttpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(),
                                                 (X509TrustManager)trustManagerFactory.getTrustManagers()[0]);
        } else if (serverUri.getScheme().equalsIgnoreCase("https")) {
            // No truststore, but we want https anyway? Better be only for test!
        }

        // Disable redirect
        okHttpClientBuilder.followRedirects(false);

        // the external hostname is not the one presented by the certificate, the hostname validation fails
        // -> we add an exception if the hostname is services and the certificate issued for v-theoden
        okHttpClientBuilder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                try {
                    String peer = sslSession.getPeerHost();
                    if ("services.sad.it".equals(peer))
                        return true;
                } catch (Exception ignored) {}
                return OkHostnameVerifier.INSTANCE.verify(s, sslSession);
            }
        });

        okHttpClient = okHttpClientBuilder.build();
    }

    private String generateUrl(RestRequest restRequest) throws URISyntaxException {
        String baseUrl = serverUri.resolve(new URI(restRequest.getAction())).toString();

        HttpUrl url = HttpUrl.parse(baseUrl);
        HttpUrl.Builder urlBuilder = url.newBuilder();
        Map<String, String> params = restRequest.getParams();
        if (params != null) {
            for (String parKey : params.keySet()) {
                urlBuilder.addQueryParameter(parKey, params.get(parKey));
            }
        }
        return urlBuilder.build().toString();
    }

    // make a request
    private RestResponse sendRequest(RestRequest restRequest) throws URISyntaxException, IOException {
        String requestUrl = generateUrl(restRequest);
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.header("User-Agent", "OkHttp RestClient").addHeader("Accept", "application/json");

        for (Map.Entry<String, String> header: restRequest.getHeaders().entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }

        // Add basic auth directly to the relevant headers
        if (username != null && password != null) {
            String credentials = Credentials.basic(username, password);
            requestBuilder.header("Authorization", credentials);
        }

        requestBuilder.url(requestUrl);

        // Build request to send to our REST service
        // Creation of PUT or POST body
        RequestBody body;
        if (restRequest.getContent() != null) {
            body = RequestBody.create(JSON, restRequest.getContent());
        } else {
            body = RequestBody.create(JSON, "");
        }

        switch (restRequest.getVerb()) {
            case GET:
            case DELETE:
                break;
            case PUT:
                requestBuilder.put(body);
                break;
            case POST:
                requestBuilder.post(body);
                break;
            default:
                break;
        }

        Request request = requestBuilder.build();

        // Send request to server
        Response response = okHttpClient.newCall(request).execute();

        String responseBody;
        int statusCode = response.code();

        if (statusCode == 307)  //Temporary redirect
            responseBody = response.header("Location");
        else
            responseBody = response.body().string();

        return new RestResponse(statusCode, responseBody);
    }

    // Make smart request, with retries and circuit breaker.
    // Exponential wait between two consecutive attempts (Fibonacci with up to maxRetryTime) until reaching maxRetries,
    // then it sets the circuit breaker to OPEN (no request allowed for circuitBreakerOpenSince ms.
    private RestResponse retrySend(final RestRequest restRequest) {
        if (retryCircuitBreakerState != RetryCircuitBreakerState.CLOSED)
            throw new IllegalArgumentException("Cannot make smart request when state = " + retryCircuitBreakerState);

        RetryerBuilder<RestResponse> builder =
                RetryerBuilder.<RestResponse>newBuilder()
                              .retryIfResult(new Predicate<RestResponse>() {
                                  @Override
                                  public boolean apply(RestResponse restResponse) {
                                      return !restResponse.isOk() && restResponse.isTransientErrorCode();
                                  }
                              })
                              .retryIfRuntimeException()
                              .withWaitStrategy(
                                      WaitStrategies.exponentialWait(100, maxRetryTime, TimeUnit.MILLISECONDS))
                              .withStopStrategy(StopStrategies.stopAfterAttempt(retries));

        for (Class<? extends Throwable> exc: RestResponse.getTransientExceptions()) {
            builder.retryIfExceptionOfType(exc);
        }

        Retryer<RestResponse> retryer = builder.build();

        RestResponse response;
        try {
            response = retryer.call(new Callable<RestResponse>() {
                @Override
                public RestResponse call() throws Exception {
                    return sendRequest(restRequest);
                }
            });
        } catch (RetryException e) {
            response = new RestResponse(e);
            openCircuitBreaker();
        } catch (ExecutionException e) {
            response = new RestResponse(e);
        }

        return response;
    }

    // depending on the value of retries it makes either
    // 1) a simple request (w/o retries, circuit breaker) if retries > 0
    // or 2) a smart request (w/ retries, circuit breaker) if retries == 0
    public RestResponse doRequest(final RestRequest restRequest) throws URISyntaxException, IOException {
        RestResponse response = null;

        switch (retryCircuitBreakerState) {
            case OFF:
                // Simple Mode is set without retries and circuit breaker
                response = sendRequest(restRequest);
                break;

            case CLOSED:
                // Smart Mode is set with retries and circuit breaker
                // No request was yet issued or the last request succeeded -> go ahead with Smart Mode
                response = retrySend(restRequest);
                break;

            case OPEN:
                // Smart Mode is set with retries and circuit breaker
                // Last request was not successful -> we either block this request or stay very careful
                long elapsedTime = System.currentTimeMillis() - circuitBreakerOpenSince;

                if (elapsedTime < maxCircuitBreakerOpenTime) {
                    // we still do not allow any requests -> throw exception
                    response = new RestResponse(new CircuitBreakerException(
                            "Requests are not permitted for another " + elapsedTime +
                            "ms because the last request failed"));

                } else {
                    try {
                        // we do allow now requests, but we want to be careful -> use Simple Mode once,
                        // if that succeeds we switch back to Smart Mode
                        response = sendRequest(restRequest);

                        // if this request succeeded we switch back to the normal Smart Mode
                        closeCircuitBreaker();

                    } catch (Exception e) {
                        response = new RestResponse(e);
                        break;
                    }
                }

            default:
                break;
        }
        return response;
    }

    public String get(String command) throws IOException, URISyntaxException {
        return get(command, null);
    }

    public String get(String command, Hashtable<String, String> params) throws URISyntaxException, IOException {
        RestResponse response = new RestRequest(this, GET, command, params).doRequest();

        if (!response.isOk())
            throw new UnsupportedOperationException(response.getData());

        return response.getData();
    }

    public int post(String command, String content) throws URISyntaxException, IOException {
        return post(command, null, content);
    }

    public int post(String command, Map<String, String> params, String content)
            throws URISyntaxException, IOException {
        return postResponse(command, params, content, Collections.<String, String>emptyMap()).getCode();
    }

    public int post(String command, String body, Map<String, String> headers) throws IOException, URISyntaxException {
        return postResponse(command, Collections.<String, String>emptyMap(), body, headers).getCode();
    }

    public RestResponse postResponse(String command, String content) throws URISyntaxException, IOException {
        return postResponse(command, Collections.<String, String>emptyMap(), content,
                            Collections.<String, String>emptyMap());
    }

    public RestResponse postResponse(String command, Map<String, String> params, String content,
                                     Map<String, String> headers)
            throws URISyntaxException, IOException {
        return new RestRequest(this, POST, command, params, content, headers).doRequest();
    }
}
