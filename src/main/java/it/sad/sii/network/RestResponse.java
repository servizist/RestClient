package it.sad.sii.network;

/**
 * Created by ldematte on 7/2/14.
 */
public class RestResponse {
    private String data = null;
    private int code = 0;
    private boolean success = false;

    public RestResponse(int code, String data) {
        this.data = data;
        this.code = code;
        this.success = (code / 100 == 2) || (code == 307); //200 family or redirect
    }

    public RestResponse(Exception ex) {
        this.success = false;
        this.data = ex.getLocalizedMessage();
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
