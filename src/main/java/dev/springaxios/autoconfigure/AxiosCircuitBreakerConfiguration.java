package dev.springaxios.autoconfigure;

import dev.springaxios.CallWrapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.concurrent.Callable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "io.github.resilience4j.circuitbreaker.CircuitBreaker")
public class AxiosCircuitBreakerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "axios.circuit-breaker", name = "enabled", havingValue = "true")
    public CallWrapper axiosCircuitBreaker(AxiosProperties props) {
        AxiosProperties.CircuitBreaker p = props.getCircuitBreaker();
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(p.getFailureRateThreshold())
                .slowCallDurationThreshold(Duration.ofMillis(p.getSlowCallDurationThresholdMs()))
                .waitDurationInOpenState(Duration.ofMillis(p.getWaitDurationInOpenStateMs()))
                .slidingWindowSize(p.getSlidingWindowSize())
                .permittedNumberOfCallsInHalfOpenState(p.getPermittedNumberOfCallsInHalfOpenState())
                .build();
        CircuitBreaker cb = CircuitBreaker.of(p.getName(), config);
        return (Callable<?> callable) -> cb.executeCallable((Callable<Object>) callable);

    }
}
