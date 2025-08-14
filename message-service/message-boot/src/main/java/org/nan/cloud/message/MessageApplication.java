package org.nan.cloud.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

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
 * @author Nan
 */
@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
        "org.nan.cloud.core.api.feign"
})
public class MessageApplication {

    public static void main(String[] args) {
        // 添加启动前的系统信息
        log.info("========== 消息服务启动开始 ==========");
        log.info("Java版本: {}", System.getProperty("java.version"));
        log.info("操作系统: {}", System.getProperty("os.name"));
        log.info("工作目录: {}", System.getProperty("user.dir"));
        
        try {
            SpringApplication.run(MessageApplication.class, args);
            
            log.info("🚀 消息服务 (Message Service) 启动成功! 端口: 8084");
            log.info("📡 WebSocket端点: ws://localhost:8084/ws");
            log.info("📋 管理端点: http://localhost:8084/actuator");
            log.info("🔧 WebSocket健康检查: http://localhost:8084/api/health/websocket");
            log.info("🔧 WebSocket调试页面: http://localhost:8084/debug-websocket.html");
            log.info("📚 API文档: http://localhost:8084/swagger-ui.html");
            log.info("========== 消息服务启动完成 ==========");
            
        } catch (Exception e) {
            log.error("❌ 消息服务启动失败！", e);
            System.exit(1);
        }
    }
}