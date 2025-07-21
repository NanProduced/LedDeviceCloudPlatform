package org.nan.cloud.message.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * RabbitMQ健康检查控制器
 * 
 * 提供RabbitMQ连接状态和配置信息检查
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/health/rabbitmq")
@RequiredArgsConstructor
public class RabbitMQHealthController {
    
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${spring.rabbitmq.host:未配置}")
    private String host;
    
    @Value("${spring.rabbitmq.port:5672}")
    private int port;
    
    @Value("${spring.rabbitmq.username:未配置}")
    private String username;
    
    @Value("${spring.rabbitmq.virtual-host:/}")
    private String virtualHost;
    
    /**
     * RabbitMQ连接健康检查
     */
    @GetMapping
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 测试连接
            rabbitTemplate.execute(channel -> {
                log.debug("RabbitMQ连接测试成功");
                return null;
            });
            
            health.put("status", "UP");
            health.put("message", "RabbitMQ连接正常");
            
            // 连接信息
            Map<String, Object> connection = new HashMap<>();
            connection.put("host", host);
            connection.put("port", port);
            connection.put("username", username);
            connection.put("virtualHost", virtualHost);
            health.put("connection", connection);
            
            log.info("RabbitMQ健康检查通过");
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("message", "RabbitMQ连接失败: " + e.getMessage());
            health.put("error", e.getClass().getSimpleName());
            
            log.error("RabbitMQ健康检查失败", e);
        }
        
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
    
    /**
     * RabbitMQ配置信息
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("host", host);
        config.put("port", port);
        config.put("username", username);
        config.put("virtualHost", virtualHost);
        config.put("managementUrl", String.format("http://%s:15672", host));
        
        // VHost配置说明
        Map<String, String> vhostGuide = new HashMap<>();
        vhostGuide.put("currentVHost", virtualHost);
        vhostGuide.put("recommendedVHost", "/led-platform-local");
        vhostGuide.put("managementUrl", String.format("http://%s:15672/#/vhosts", host));
        vhostGuide.put("createCommand", "rabbitmqctl add_vhost /led-platform-local");
        vhostGuide.put("permissionCommand", String.format("rabbitmqctl set_permissions -p /led-platform-local %s \".*\" \".*\" \".*\"", username));
        
        config.put("vhostGuide", vhostGuide);
        config.put("timestamp", System.currentTimeMillis());
        
        return config;
    }
    
    /**
     * 测试队列创建
     */
    @GetMapping("/test-queue")
    public Map<String, Object> testQueueCreation() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String testQueueName = "test-queue-" + System.currentTimeMillis();
            
            // 尝试创建临时队列
            rabbitTemplate.execute(channel -> {
                channel.queueDeclare(testQueueName, false, false, true, null);
                log.debug("测试队列创建成功: {}", testQueueName);
                
                // 立即删除测试队列
                channel.queueDelete(testQueueName);
                log.debug("测试队列删除成功: {}", testQueueName);
                
                return null;
            });
            
            result.put("status", "SUCCESS");
            result.put("message", "队列创建和删除测试通过");
            result.put("testQueue", testQueueName);
            
            log.info("RabbitMQ队列操作测试通过");
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", "队列操作测试失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            log.error("RabbitMQ队列操作测试失败", e);
        }
        
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}