package dev.springaxios;

import java.util.Collections;
import java.util.Map;

/**
 * Thrown when a request fails: connection error, deserialization error, or an
 * HTTP error status. Carries the partial response info when available.
 */
public class AxiosException extends RuntimeException {

    private final int status;
    private final String statusText;
    private final Map<String, String> headers;
    private final byte[] body;

    public AxiosException(String message, Throwable cause) {
        this(0, "", Collections.emptyMap(), null, message, cause);
    }

    public AxiosException(int status, String statusText, Map<String, String> headers, byte[] body, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.statusText = statusText;
        this.headers = headers;
        this.body = body;
    }

    public int getStatus() { return status; }
    public String getStatusText() { return statusText; }
    public Map<String, String> getHeaders() { return headers; }
    public byte[] getBody() { return body; }
}
