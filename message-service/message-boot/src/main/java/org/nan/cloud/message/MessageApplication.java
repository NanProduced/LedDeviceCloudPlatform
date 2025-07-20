package org.nan.cloud.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 消息服务启动类
 * 
 * 功能说明：
 * - 统一的WebSocket连接管理中心
 * - 基于RabbitMQ的消息队列处理
 * - MongoDB消息持久化存储
 * - Redis在线用户状态管理
 * - 多租户消息隔离
 * - 事件驱动的消息分发
 * 
 * 服务端口：8084
 * 
 * @author LedDeviceCloudPlatform
 * @since 2025-01-20
 */
@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
public class MessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageApplication.class, args);
        log.info("🚀 消息服务 (Message Service) 启动成功! 端口: 8084");
        log.info("📡 WebSocket端点: ws://localhost:8084/ws");
        log.info("📋 管理端点: http://localhost:8084/actuator");
        log.info("📚 API文档: http://localhost:8084/swagger-ui.html");
    }
}