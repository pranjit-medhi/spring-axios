package dev.springaxios;

import java.util.concurrent.Callable;

/**
 * Wraps the execution of a request. Used to plug in cross-cutting behavior such as
 * circuit breaking or additional retry policies without coupling the core to any
 * specific library.
 *
 * @param <T> the response type
 */
@FunctionalInterface
public interface CallWrapper {
    Object wrap(Callable<?> callable) throws Exception;
}
