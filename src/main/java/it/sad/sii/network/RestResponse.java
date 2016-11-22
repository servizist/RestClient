package it.sad.sii.network;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by ldematte on 7/2/14.
 */
public class RestResponse {
    private String data = null;
    private Exception exception = null;
    private int code = 0;
    private boolean success = false;

    public RestResponse(int code, String data) {
        this.data = data;
        this.code = code;
        this.exception = null;
        this.success = (code / 100 == 2) || (code == 307); //200 family or redirect
    }

    public RestResponse(Exception ex) {
        this.success = false;
        // Initialize error codes with some which will reflect permanent VS transient failure.
        this.code = isTransientException(ex) ? 500 : 400;
        this.exception = ex;
        this.data = ex.getLocalizedMessage();
    }

    // I just list which codes are to be considered permanent.
    // Better be safe, and err on the "we'll retry" side.
    // 400 Bad Request: per spec, the client SHOULD NOT repeat the request without modifications.
    // 401 Not authorized: we are not interactive, so trying again (with the same credentials)
    //                     will do no good.
    // 403 Forbidden: authorization will not help and the request SHOULD NOT be repeated.
    // 404 Not found
    // 410 Gone: this condition is expected to be considered permanent
    // 415 Unsupported Media Type
    // 501 Not Implemented
    // IMPORTANT: keep them ordered!
    private final static int[] permanentErrorCodes = new int[] { 400, 401, 403, 404, 410, 415, 501 };

    // Unfortunately, OkHttp just declares to throw "IOException", so
    private final static List<Class<? extends Throwable>> transientExceptions =
            Collections.<Class<? extends Throwable>>singletonList(IOException.class);

    public boolean isTransientErrorCode() {
        return Arrays.binarySearch(permanentErrorCodes, this.code) < 0;
    }

    public static List<Class<? extends Throwable>> getTransientExceptions() {
        return transientExceptions;
    }

    public static boolean isTransientException(Exception ex) {
        if (ex != null) {
            for (Class<? extends Throwable> exc: transientExceptions) {
                if (exc.isAssignableFrom(ex.getClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isTransientException() {
        return isTransientException(this.exception);
    }

    public boolean isTransientFailure() {
        return !success &&
               (isTransientErrorCode() || isTransientException());
    }

    public String getData() {
        return data;
    }

    public int getCode() {
        return code;
    }

    public boolean isOk() {
        return success;
    }
}
