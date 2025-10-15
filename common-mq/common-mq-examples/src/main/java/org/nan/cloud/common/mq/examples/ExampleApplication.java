package org.nan.cloud.common.mq.examples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例应用程序
 * 
 * 演示如何使用通用RabbitMQ客户端模块。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@SpringBootApplication
public class ExampleApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}