package org.nan.cloud.message.config;

import org.nan.cloud.message.config.MessageProperties;
import org.nan.cloud.message.config.WebSocketProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * 消息服务配置属性启用类
 * 
 * 使用@EnableConfigurationProperties注解来启用配置属性类，
 * 确保Spring Boot能够正确识别和加载这些配置类。
 * 
 * 注意：配置属性类定义在application层，Boot层只负责启用它们，
 * 这样遵循了DDD分层架构原则。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties({
    WebSocketProperties.class,
    MessageProperties.class
})
public class MessageConfigurationProperties {
    
    // 这个类不需要任何实现代码
    // 它的作用是通过@EnableConfigurationProperties注解
    // 告诉Spring Boot扫描和启用application层定义的配置属性类
}