package dev.springaxios;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.springaxios.interceptor.RequestInterceptor;
import dev.springaxios.interceptor.ResponseInterceptor;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Axios-like HTTP client for Spring Boot.
 *
 * <p>Supports fluent sync/async calls, request/response interceptors, per-request and
 * instance-level defaults, JSON (de)serialization, timeouts, retries with backoff,
 * Micrometer metrics and an optional Resilience4j circuit breaker.</p>
 */
public class AxiosClient {

    private final String baseUrl;
    private final int timeoutMs;
    private final int maxRetries;
    private final long retryDelayMs;
    private final double retryBackoffMultiplier;
    private final List<Integer> retryableStatuses;
    private final Map<String, String> defaultHeaders;
    private final List<RequestInterceptor> requestInterceptors;
    private final List<ResponseInterceptor> responseInterceptors;
    private final ObjectMapper objectMapper;
    private final MetricsRecorder metricsRecorder;
    private final CallWrapper callWrapper;

    private final Map<Integer, RestClient> clientCache = new ConcurrentHashMap<>();
    private Executor asyncExecutor = ForkJoinPool.commonPool();

    public AxiosClient(AxiosConfig config, ObjectMapper objectMapper, MetricsRecorder metricsRecorder, CallWrapper callWrapper) {
        this.baseUrl = config.getBaseUrl();
        this.timeoutMs = config.getTimeoutMs();
        this.maxRetries = config.getMaxRetries();
        this.retryDelayMs = config.getRetryDelayMs();
        this.retryBackoffMultiplier = config.getRetryBackoffMultiplier();
        this.retryableStatuses = config.getRetryableStatuses();
        this.defaultHeaders = new LinkedHashMap<>(config.getDefaultHeaders());
        this.requestInterceptors = new ArrayList<>(config.getRequestInterceptors());
        this.responseInterceptors = new ArrayList<>(config.getResponseInterceptors());
        this.objectMapper = config.getObjectMapper() != null ? config.getObjectMapper() : objectMapper;
        this.metricsRecorder = metricsRecorder;
        this.callWrapper = config.getCallWrapper() != null ? config.getCallWrapper() : callWrapper;
    }

    public static AxiosClient create() {
        return new AxiosClient(AxiosConfig.builder().build(), new ObjectMapper(), null, null);
    }

    public static AxiosClient create(AxiosConfig config) {
        return new AxiosClient(config, new ObjectMapper(), null, null);
    }

    public static AxiosClient create(AxiosConfig config, ObjectMapper objectMapper, MetricsRecorder metricsRecorder, CallWrapper callWrapper) {
        return new AxiosClient(config, objectMapper, metricsRecorder, callWrapper);
    }

    public void setAsyncExecutor(Executor executor) {
        this.asyncExecutor = executor;
    }

    /** Creates a derived client whose defaults are merged on top of this one. */
    public AxiosClient createInstance(AxiosConfig overrides) {
        AxiosConfig merged = AxiosConfig.builder()
                .baseUrl(overrides.getBaseUrl() != null ? overrides.getBaseUrl() : this.baseUrl)
                .timeoutMs(overrides.getTimeoutMs() != 0 ? overrides.getTimeoutMs() : this.timeoutMs)
                .maxRetries(overrides.getMaxRetries() != 0 ? overrides.getMaxRetries() : this.maxRetries)
                .retryDelayMs(overrides.getRetryDelayMs() != 0 ? overrides.getRetryDelayMs() : this.retryDelayMs)
                .retryBackoffMultiplier(overrides.getRetryBackoffMultiplier() != 0 ? overrides.getRetryBackoffMultiplier() : this.retryBackoffMultiplier)
                .retryableStatuses(overrides.getRetryableStatuses() != null ? overrides.getRetryableStatuses() : this.retryableStatuses)
                .defaultHeaders(new LinkedHashMap<>(this.defaultHeaders))
                .requestInterceptors(new ArrayList<>(this.requestInterceptors))
                .responseInterceptors(new ArrayList<>(this.responseInterceptors))
                .build();
        merged.getDefaultHeaders().putAll(overrides.getDefaultHeaders());
        merged.getRequestInterceptors().addAll(overrides.getRequestInterceptors());
        merged.getResponseInterceptors().addAll(overrides.getResponseInterceptors());
        ObjectMapper om = overrides.getObjectMapper() != null ? overrides.getObjectMapper() : this.objectMapper;
        CallWrapper w = overrides.getCallWrapper() != null ? overrides.getCallWrapper() : this.callWrapper;
        return new AxiosClient(merged, om, this.metricsRecorder, w);
    }

    // ---- convenience verbs ----

    public <T> AxiosResponse<T> get(String url, Class<T> type) {
        return request(AxiosRequest.builder().method("GET").url(url).responseType(type).build());
    }

    public <T> AxiosResponse<T> get(String url, Class<T> type, AxiosConfig overrides) {
        return request(AxiosRequest.builder().method("GET").url(url).responseType(type).build(), overrides);
    }

    public <T> AxiosResponse<T> get(String url, TypeReference<T> ref) {
        return request(AxiosRequest.builder().method("GET").url(url).responseTypeRef(ref).build());
    }

    public <T> AxiosResponse<T> post(String url, Object body, Class<T> type) {
        return request(AxiosRequest.builder().method("POST").url(url).body(body).responseType(type).build());
    }

    public <T> AxiosResponse<T> put(String url, Object body, Class<T> type) {
        return request(AxiosRequest.builder().method("PUT").url(url).body(body).responseType(type).build());
    }

    public <T> AxiosResponse<T> patch(String url, Object body, Class<T> type) {
        return request(AxiosRequest.builder().method("PATCH").url(url).body(body).responseType(type).build());
    }

    public <T> AxiosResponse<T> delete(String url, Class<T> type) {
        return request(AxiosRequest.builder().method("DELETE").url(url).responseType(type).build());
    }

    public AxiosResponse<Void> delete(String url) {
        return request(AxiosRequest.builder().method("DELETE").url(url).responseType(Void.class).build());
    }

    public <T> CompletableFuture<AxiosResponse<T>> requestAsync(AxiosRequest req) {
        return CompletableFuture.supplyAsync(() -> request(req), asyncExecutor);
    }

    public <T> CompletableFuture<AxiosResponse<T>> getAsync(String url, Class<T> type) {
        return requestAsync(AxiosRequest.builder().method("GET").url(url).responseType(type).build());
    }

    // ---- core ----

    public <T> AxiosResponse<T> request(AxiosRequest raw) {
        return run(raw, null);
    }

    public <T> AxiosResponse<T> request(AxiosRequest raw, AxiosConfig overrides) {
        return run(raw, overrides);
    }

    private <T> AxiosResponse<T> run(AxiosRequest raw, AxiosConfig overrides) {
        Resolved c = resolve(overrides);

        AxiosRequest req = raw;
        for (RequestInterceptor i : c.requestInterceptors) {
            req = i.intercept(req);
        }

        Map<String, String> merged = new LinkedHashMap<>(c.defaultHeaders);
        merged.putAll(req.getHeaders());

        AxiosRequest finalReq = AxiosRequest.builder()
                .method(req.getMethod())
                .url(req.getUrl())
                .baseUrl(c.baseUrl)
                .body(req.getBody())
                .headers(merged)
                .queryParams(req.getQueryParams())
                .pathVars(req.getPathVars())
                .responseType(req.getResponseType())
                .responseTypeRef(req.getResponseTypeRef())
                .timeoutMs(req.getTimeoutMs() != 0 ? req.getTimeoutMs() : c.timeoutMs)
                .maxRetries(req.getMaxRetries() != 0 ? req.getMaxRetries() : c.maxRetries)
                .retryDelayMs(req.getRetryDelayMs() != 0 ? req.getRetryDelayMs() : c.retryDelayMs)
                .retryBackoffMultiplier(req.getRetryBackoffMultiplier() != 0 ? req.getRetryBackoffMultiplier() : c.backoff)
                .retryableStatuses(req.getRetryableStatuses() != null ? req.getRetryableStatuses() : c.retryableStatuses)
                .build();

        Callable<AxiosResponse<T>> task = () -> doWithRetry(finalReq, c.objectMapper);
        Callable<AxiosResponse<T>> withMetrics = c.metricsRecorder != null
                ? () -> (AxiosResponse<T>) c.metricsRecorder.record(finalReq.getMethod(), hostOf(finalReq), task)
                : task;

        AxiosResponse<T> resp;
        if (c.callWrapper != null) {
            try {
                resp = (AxiosResponse<T>) c.callWrapper.wrap(withMetrics);
            } catch (Exception e) {
                throw unwrap(e);
            }
        } else {
            try {
                resp = withMetrics.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new AxiosException("Unexpected error during request", e);
            }
        }

        for (ResponseInterceptor i : c.responseInterceptors) {
            resp = (AxiosResponse<T>) i.intercept(resp);
        }
        return resp;
    }

    private <T> AxiosResponse<T> doWithRetry(AxiosRequest req, ObjectMapper om) {
        int max = req.getMaxRetries();
        long delay = req.getRetryDelayMs();
        double backoff = req.getRetryBackoffMultiplier();
        List<Integer> retryable = req.getRetryableStatuses();
        int attempt = 0;
        while (true) {
            try {
                AxiosResponse<T> r = doSend(req, om);
                if (retryable != null && r.getStatus() != 0 && retryable.contains(r.getStatus()) && attempt < max) {
                    attempt++;
                    sleep(delay);
                    delay = (long) (delay * backoff);
                    continue;
                }
                return r;
            } catch (ResourceAccessException ex) {
                if (attempt < max) {
                    attempt++;
                    sleep(delay);
                    delay = (long) (delay * backoff);
                    continue;
                }
                throw new AxiosException(0, "I/O error: " + ex.getMessage(), Collections.emptyMap(), null, ex.getMessage(), ex);
            }
        }
    }

    private <T> AxiosResponse<T> doSend(AxiosRequest req, ObjectMapper om) {
        RestClient rc = getRestClient(req.getTimeoutMs());
        String uri = buildUri(req, req.getBaseUrl());
        RestClient.RequestBodyUriSpec spec = rc.method(HttpMethod.valueOf(req.getMethod().toUpperCase(Locale.ROOT)));
        spec.uri(uri);
        req.getHeaders().forEach(spec::header);
        if (req.getBody() != null) {
            if (req.getBody() instanceof String s) {
                spec.body(s);
            } else if (req.getBody() instanceof byte[] bytes) {
                spec.body(bytes);
            } else {
                spec.contentType(MediaType.APPLICATION_JSON);
                spec.body(req.getBody());
            }
        }
        ResponseEntity<byte[]> re = spec.retrieve()
                .onStatus(HttpStatusCode::isError, (rq, rs) -> {
                })
                .toEntity(byte[].class);

        int status = re.getStatusCode().value();
        HttpStatus hs = HttpStatus.resolve(status);
        String statusText = hs != null ? hs.name() : "";
        Map<String, String> hm = new HashMap<>();
        re.getHeaders().forEach((k, v) -> hm.put(k, v.isEmpty() ? "" : v.get(0)));
        T data = deserialize(re.getBody(), req, om);
        return AxiosResponse.of(status, statusText, hm, data, req);
    }

    private <T> T deserialize(byte[] body, AxiosRequest req, ObjectMapper om) {
        if (body == null || body.length == 0) {
            return null;
        }
        if (req.getResponseTypeRef() != null) {
            try {
                return (T) om.readValue(body, om.getTypeFactory().constructType(req.getResponseTypeRef().getType()));
            } catch (Exception e) {
                throw new AxiosException("Failed to deserialize response body", e);
            }
        }
        Class<?> type = req.getResponseType();
        if (type == null || type == byte[].class) {
            return (T) body;
        }
        if (type == Void.class) {
            return null;
        }
        if (type == String.class) {
            return (T) new String(body, StandardCharsets.UTF_8);
        }
        try {
            return (T) om.readValue(body, type);
        } catch (Exception e) {
            throw new AxiosException("Failed to deserialize response body", e);
        }
    }

    private String buildUri(AxiosRequest req, String baseUrl) {
        String url = req.getUrl();
        String full;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            full = url;
        } else if (baseUrl != null && !baseUrl.isEmpty()) {
            String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String u = url.startsWith("/") ? url : "/" + url;
            full = b + u;
        } else {
            full = url;
        }
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(full);
        req.getQueryParams().forEach((k, v) -> b.queryParam(k, v));
        return b.buildAndExpand(req.getPathVars()).toUriString();
    }

    private RestClient getRestClient(int timeoutMs) {
        return clientCache.computeIfAbsent(timeoutMs, t -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(t);
            factory.setReadTimeout(t);
            return RestClient.builder().requestFactory(factory).build();
        });
    }

    private String hostOf(AxiosRequest req) {
        try {
            String u = req.getUrl();
            if (req.getBaseUrl() != null) {
                u = req.getBaseUrl().replaceAll("/$", "") + (u.startsWith("/") ? u : "/" + u);
            }
            return URI.create(u).getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Resolved resolve(AxiosConfig overrides) {
        String base = (overrides != null && overrides.getBaseUrl() != null) ? overrides.getBaseUrl() : this.baseUrl;
        int timeout = (overrides != null && overrides.getTimeoutMs() != 0) ? overrides.getTimeoutMs() : this.timeoutMs;
        int retries = (overrides != null && overrides.getMaxRetries() != 0) ? overrides.getMaxRetries() : this.maxRetries;
        long delay = (overrides != null && overrides.getRetryDelayMs() != 0) ? overrides.getRetryDelayMs() : this.retryDelayMs;
        double backoff = (overrides != null && overrides.getRetryBackoffMultiplier() != 0) ? overrides.getRetryBackoffMultiplier() : this.retryBackoffMultiplier;
        List<Integer> retryable = (overrides != null && overrides.getRetryableStatuses() != null) ? overrides.getRetryableStatuses() : this.retryableStatuses;
        Map<String, String> defHeaders = new LinkedHashMap<>(this.defaultHeaders);
        if (overrides != null) {
            defHeaders.putAll(overrides.getDefaultHeaders());
        }
        List<RequestInterceptor> ris = new ArrayList<>(this.requestInterceptors);
        List<ResponseInterceptor> ros = new ArrayList<>(this.responseInterceptors);
        if (overrides != null) {
            ris.addAll(overrides.getRequestInterceptors());
            ros.addAll(overrides.getResponseInterceptors());
        }
        ObjectMapper om = (overrides != null && overrides.getObjectMapper() != null) ? overrides.getObjectMapper() : this.objectMapper;
        CallWrapper w = (overrides != null && overrides.getCallWrapper() != null) ? overrides.getCallWrapper() : this.callWrapper;
        return new Resolved(base, timeout, retries, delay, backoff, retryable, defHeaders, ris, ros, om, this.metricsRecorder, w);
    }

    private RuntimeException unwrap(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new AxiosException("Request failed inside circuit breaker", e);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private record Resolved(
            String baseUrl,
            int timeoutMs,
            int maxRetries,
            long retryDelayMs,
            double backoff,
            List<Integer> retryableStatuses,
            Map<String, String> defaultHeaders,
            List<RequestInterceptor> requestInterceptors,
            List<ResponseInterceptor> responseInterceptors,
            ObjectMapper objectMapper,
            MetricsRecorder metricsRecorder,
            CallWrapper callWrapper) {
    }
}
