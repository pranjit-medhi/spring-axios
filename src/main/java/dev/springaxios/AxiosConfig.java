package dev.springaxios;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.springaxios.interceptor.RequestInterceptor;
import dev.springaxios.interceptor.ResponseInterceptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable-ish configuration carrying the defaults for an {@link AxiosClient} instance.
 * Mirrors axios "instance defaults" (baseURL, headers, timeout, auth...).
 */
public class AxiosConfig {

    private String baseUrl;
    private final Map<String, String> defaultHeaders = new HashMap<>();
    private int timeoutMs = 10_000;
    private int maxRetries = 0;
    private long retryDelayMs = 500;
    private double retryBackoffMultiplier = 2.0;
    private List<Integer> retryableStatuses = List.of(429, 500, 502, 503, 504);

    private final List<RequestInterceptor> requestInterceptors = new ArrayList<>();
    private final List<ResponseInterceptor> responseInterceptors = new ArrayList<>();

    private ObjectMapper objectMapper;
    private CallWrapper callWrapper;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AxiosConfig config = new AxiosConfig();

        public Builder baseUrl(String baseUrl) { config.baseUrl = baseUrl; return this; }
        public Builder defaultHeader(String name, String value) { config.defaultHeaders.put(name, value); return this; }
        public Builder defaultHeaders(Map<String, String> headers) { config.defaultHeaders.putAll(headers); return this; }
        public Builder timeoutMs(int timeoutMs) { config.timeoutMs = timeoutMs; return this; }
        public Builder maxRetries(int maxRetries) { config.maxRetries = maxRetries; return this; }
        public Builder retryDelayMs(long retryDelayMs) { config.retryDelayMs = retryDelayMs; return this; }
        public Builder retryBackoffMultiplier(double m) { config.retryBackoffMultiplier = m; return this; }
        public Builder retryableStatuses(List<Integer> statuses) { config.retryableStatuses = statuses; return this; }
        public Builder requestInterceptor(RequestInterceptor i) { config.requestInterceptors.add(i); return this; }
        public Builder requestInterceptors(List<RequestInterceptor> list) { config.requestInterceptors.addAll(list); return this; }
        public Builder responseInterceptor(ResponseInterceptor i) { config.responseInterceptors.add(i); return this; }
        public Builder responseInterceptors(List<ResponseInterceptor> list) { config.responseInterceptors.addAll(list); return this; }
        public Builder objectMapper(ObjectMapper objectMapper) { config.objectMapper = objectMapper; return this; }
        public Builder callWrapper(CallWrapper callWrapper) { config.callWrapper = callWrapper; return this; }

        public AxiosConfig build() { return config; }
    }

    public String getBaseUrl() { return baseUrl; }
    public Map<String, String> getDefaultHeaders() { return defaultHeaders; }
    public int getTimeoutMs() { return timeoutMs; }
    public int getMaxRetries() { return maxRetries; }
    public long getRetryDelayMs() { return retryDelayMs; }
    public double getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public List<Integer> getRetryableStatuses() { return retryableStatuses; }
    public List<RequestInterceptor> getRequestInterceptors() { return requestInterceptors; }
    public List<ResponseInterceptor> getResponseInterceptors() { return responseInterceptors; }
    public ObjectMapper getObjectMapper() { return objectMapper; }
    public CallWrapper getCallWrapper() { return callWrapper; }
}
