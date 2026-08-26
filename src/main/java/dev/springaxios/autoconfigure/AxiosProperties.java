package dev.springaxios.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "axios")
public class AxiosProperties {

    private String baseUrl;
    private java.util.Map<String, String> defaultHeaders = new java.util.LinkedHashMap<>();
    private int timeoutMs = 10_000;
    private int maxRetries = 0;
    private long retryDelayMs = 500;
    private double retryBackoffMultiplier = 2.0;
    private List<Integer> retryableStatuses = List.of(429, 500, 502, 503, 504);
    private boolean metricsEnabled = true;
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    public static class CircuitBreaker {
        private boolean enabled = false;
        private String name = "axios";
        private int failureRateThreshold = 50;
        private int slowCallDurationThresholdMs = 10_000;
        private int waitDurationInOpenStateMs = 5_000;
        private int slidingWindowSize = 100;
        private int permittedNumberOfCallsInHalfOpenState = 10;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getFailureRateThreshold() { return failureRateThreshold; }
        public void setFailureRateThreshold(int failureRateThreshold) { this.failureRateThreshold = failureRateThreshold; }
        public int getSlowCallDurationThresholdMs() { return slowCallDurationThresholdMs; }
        public void setSlowCallDurationThresholdMs(int slowCallDurationThresholdMs) { this.slowCallDurationThresholdMs = slowCallDurationThresholdMs; }
        public int getWaitDurationInOpenStateMs() { return waitDurationInOpenStateMs; }
        public void setWaitDurationInOpenStateMs(int waitDurationInOpenStateMs) { this.waitDurationInOpenStateMs = waitDurationInOpenStateMs; }
        public int getSlidingWindowSize() { return slidingWindowSize; }
        public void setSlidingWindowSize(int slidingWindowSize) { this.slidingWindowSize = slidingWindowSize; }
        public int getPermittedNumberOfCallsInHalfOpenState() { return permittedNumberOfCallsInHalfOpenState; }
        public void setPermittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) { this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState; }
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public java.util.Map<String, String> getDefaultHeaders() { return defaultHeaders; }
    public void setDefaultHeaders(java.util.Map<String, String> defaultHeaders) { this.defaultHeaders = defaultHeaders; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    public double getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public void setRetryBackoffMultiplier(double retryBackoffMultiplier) { this.retryBackoffMultiplier = retryBackoffMultiplier; }
    public List<Integer> getRetryableStatuses() { return retryableStatuses; }
    public void setRetryableStatuses(List<Integer> retryableStatuses) { this.retryableStatuses = retryableStatuses; }
    public boolean isMetricsEnabled() { return metricsEnabled; }
    public void setMetricsEnabled(boolean metricsEnabled) { this.metricsEnabled = metricsEnabled; }
    public CircuitBreaker getCircuitBreaker() { return circuitBreaker; }
    public void setCircuitBreaker(CircuitBreaker circuitBreaker) { this.circuitBreaker = circuitBreaker; }
}
