package dev.springaxios;

import java.util.Map;

/**
 * The response object returned for every call (analogous to axios' response).
 *
 * @param <T> the deserialized body type
 */
public class AxiosResponse<T> {

    private int status;
    private String statusText;
    private Map<String, String> headers;
    private T data;
    private AxiosRequest request;

    public static <T> AxiosResponse<T> of(int status, String statusText, Map<String, String> headers, T data, AxiosRequest request) {
        AxiosResponse<T> r = new AxiosResponse<>();
        r.status = status;
        r.statusText = statusText;
        r.headers = headers;
        r.data = data;
        r.request = request;
        return r;
    }

    public int getStatus() { return status; }
    public String getStatusText() { return statusText; }
    public Map<String, String> getHeaders() { return headers; }
    public T getData() { return data; }
    public AxiosRequest getRequest() { return request; }

    public boolean isOk() { return status >= 200 && status < 300; }
}
