package dev.springaxios.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.springaxios.AxiosClient;
import dev.springaxios.AxiosConfig;
import dev.springaxios.CallWrapper;
import dev.springaxios.MetricsRecorder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.web.client.RestClient")
@EnableConfigurationProperties(AxiosProperties.class)
public class AxiosAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AxiosConfig axiosConfig(AxiosProperties props) {
        AxiosConfig.Builder builder = AxiosConfig.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeaders(props.getDefaultHeaders())
                .timeoutMs(props.getTimeoutMs())
                .maxRetries(props.getMaxRetries())
                .retryDelayMs(props.getRetryDelayMs())
                .retryBackoffMultiplier(props.getRetryBackoffMultiplier())
                .retryableStatuses(props.getRetryableStatuses());
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AxiosClient axiosClient(AxiosProperties props,
                                    AxiosConfig config,
                                    ObjectMapper objectMapper,
                                    ObjectProvider<MetricsRecorder> metrics,
                                    ObjectProvider<CallWrapper> callWrapper) {
        MetricsRecorder mr = props.isMetricsEnabled() ? metrics.getIfAvailable() : null;
        CallWrapper cw = callWrapper.getIfAvailable();
        return AxiosClient.create(config, objectMapper, mr, cw);
    }
}
