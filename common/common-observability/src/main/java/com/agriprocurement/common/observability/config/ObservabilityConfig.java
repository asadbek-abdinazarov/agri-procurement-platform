package com.agriprocurement.common.observability.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration for observability features including metrics, tracing, and logging.
 * Enables actuator endpoints and configures common tags for all metrics.
 */
@Configuration
@EnableAspectJAutoProxy
public class ObservabilityConfig {

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${observability.environment:dev}")
    private String environment;

    @Value("${observability.region:us-east-1}")
    private String region;

    @Value("${observability.version:1.0.0}")
    private String version;

    /**
     * Customizes meter registry with common tags applied to all metrics.
     * These tags help with filtering and grouping metrics in monitoring systems.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            Tags.of(
                Tag.of("application", applicationName),
                Tag.of("environment", environment),
                Tag.of("region", region),
                Tag.of("version", version)
            )
        );
    }

    /**
     * Enables support for @Timed annotation on methods.
     * Automatically records execution time metrics for annotated methods.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
