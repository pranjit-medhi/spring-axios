package dev.springaxios.interceptor;

import dev.springaxios.AxiosResponse;

/**
 * Receives the incoming {@link AxiosResponse} and may return a (possibly mutated)
 * response. Analogous to axios response interceptors.
 */
@FunctionalInterface
public interface ResponseInterceptor {
    AxiosResponse<?> intercept(AxiosResponse<?> response);
}
