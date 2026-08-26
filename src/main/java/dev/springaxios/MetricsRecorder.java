package dev.springaxios;

import java.util.concurrent.Callable;

/**
 * Records timing/counts around a request. Implementations may push to Micrometer,
 * Dropwizard, etc. Kept dependency-free so the core does not require any metrics lib.
 *
 * @param <T> the response type
 */
@FunctionalInterface
public interface MetricsRecorder {
    Object record(String method, String host, Callable<?> callable) throws Exception;
}
