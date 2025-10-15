package org.nan.cloud.message.infrastructure.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate配置
 * 
 * 配置用于Gateway认证的HTTP客户端，
 * 设置合适的超时时间和连接池参数。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * 创建RestTemplate Bean
     * 用于调用Gateway的用户信息接口
     * 
     * @return 配置好的RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}