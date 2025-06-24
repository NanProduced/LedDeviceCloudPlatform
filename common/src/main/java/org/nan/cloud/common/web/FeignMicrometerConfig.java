package org.nan.cloud.common.web;

import feign.Capability;
import feign.micrometer.MicrometerCapability;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignMicrometerConfig {

    /**
     * 让 feign-micrometer 自动把 traceparent/B3 等标准 header 加入每次调用
     */
    @Bean
    public Capability micrometerCapability(MeterRegistry registry) {
        return new MicrometerCapability(registry);
    }

    @Bean
    public feign.Logger.Level feignLoggerLevel() {
        return feign.Logger.Level.BASIC;
    }
}
