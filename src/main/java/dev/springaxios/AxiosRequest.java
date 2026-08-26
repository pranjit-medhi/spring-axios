package dev.springaxios;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single HTTP request description. Use {@link #builder()} or the convenience
 * methods on {@link AxiosClient}.
 */
public class AxiosRequest {

    private String method = "GET";
    private String url;
    private String baseUrl;
    private Object body;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, Object> queryParams = new LinkedHashMap<>();
    private final Map<String, Object> pathVars = new LinkedHashMap<>();

    private Class<?> responseType = String.class;
    private TypeReference<?> responseTypeRef;

    private int timeoutMs;
    private int maxRetries;
    private long retryDelayMs;
    private double retryBackoffMultiplier;
    private java.util.List<Integer> retryableStatuses;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AxiosRequest req = new AxiosRequest();

        public Builder method(String method) { req.method = method; return this; }
        public Builder url(String url) { req.url = url; return this; }
        public Builder baseUrl(String baseUrl) { req.baseUrl = baseUrl; return this; }
        public Builder body(Object body) { req.body = body; return this; }
        public Builder header(String name, String value) { req.headers.put(name, value); return this; }
        public Builder headers(Map<String, String> headers) { req.headers.putAll(headers); return this; }
        public Builder queryParam(String name, Object value) { req.queryParams.put(name, value); return this; }
        public Builder queryParams(Map<String, Object> params) { req.queryParams.putAll(params); return this; }
        public Builder pathVar(String name, Object value) { req.pathVars.put(name, value); return this; }
        public Builder pathVars(Map<String, Object> vars) { req.pathVars.putAll(vars); return this; }
        public Builder responseType(Class<?> type) { req.responseType = type; return this; }
        public Builder responseTypeRef(TypeReference<?> ref) { req.responseTypeRef = ref; return this; }
        public Builder timeoutMs(int t) { req.timeoutMs = t; return this; }
        public Builder maxRetries(int r) { req.maxRetries = r; return this; }
        public Builder retryDelayMs(long d) { req.retryDelayMs = d; return this; }
        public Builder retryBackoffMultiplier(double m) { req.retryBackoffMultiplier = m; return this; }
        public Builder retryableStatuses(java.util.List<Integer> s) { req.retryableStatuses = s; return this; }

        public AxiosRequest build() { return req; }
    }

    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public String getBaseUrl() { return baseUrl; }
    public Object getBody() { return body; }
    public Map<String, String> getHeaders() { return headers; }
    public Map<String, Object> getQueryParams() { return queryParams; }
    public Map<String, Object> getPathVars() { return pathVars; }
    public Class<?> getResponseType() { return responseType; }
    public TypeReference<?> getResponseTypeRef() { return responseTypeRef; }
    public int getTimeoutMs() { return timeoutMs; }
    public int getMaxRetries() { return maxRetries; }
    public long getRetryDelayMs() { return retryDelayMs; }
    public double getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public java.util.List<Integer> getRetryableStatuses() { return retryableStatuses; }
}
