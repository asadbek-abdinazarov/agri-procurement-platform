package com.agriprocurement.common.observability;

import com.agriprocurement.common.observability.config.ObservabilityConfig;
import com.agriprocurement.common.observability.metrics.BusinessMetrics;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Auto-configuration for common observability module.
 * Automatically enables metrics, tracing, and structured logging when included in classpath.
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@Import({ObservabilityConfig.class, BusinessMetrics.class})
public class ObservabilityAutoConfiguration {
}
