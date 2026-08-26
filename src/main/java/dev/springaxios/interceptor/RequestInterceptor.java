package dev.springaxios.interceptor;

import dev.springaxios.AxiosRequest;

/**
 * Receives the outgoing {@link AxiosRequest} and may return a (possibly mutated)
 * request. Analogous to axios request interceptors.
 */
@FunctionalInterface
public interface RequestInterceptor {
    AxiosRequest intercept(AxiosRequest request);
}
