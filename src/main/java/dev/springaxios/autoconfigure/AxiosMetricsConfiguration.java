package dev.springaxios.autoconfigure;

import dev.springaxios.MetricsRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.Callable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
public class AxiosMetricsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "axios", name = "metrics-enabled", havingValue = "true", matchIfMissing = true)
    public MetricsRecorder axiosMetricsRecorder(ObjectProvider<MeterRegistry> registry) {
        MeterRegistry reg = registry.getIfAvailable();
        if (reg == null) {
            return null;
        }
        return (method, host, callable) -> {
            Timer timer = Timer.builder("http.client.requests")
                    .tag("client", "axios")
                    .tag("method", method)
                    .tag("host", host == null ? "unknown" : host)
                    .register(reg);
            return timer.recordCallable(callable);
        };
    }
}
