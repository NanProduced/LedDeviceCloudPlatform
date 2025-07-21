package org.nan.cloud.message.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationContext;

/**
 * WebSocket调试配置类
 * 
 * 用于检查WebSocket相关Bean的加载情况，帮助调试连接问题
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class WebSocketDebugConfig {
    
    /**
     * 启动时检查WebSocket相关Bean的加载情况
     */
    @Bean
    public ApplicationRunner webSocketBeanChecker(ApplicationContext context) {
        return args -> {
            log.info("========== WebSocket Bean 检查开始 ==========");
            
            // 检查核心WebSocket Bean
            checkBean(context, "webSocketConfig", "WebSocketConfig");
            checkBean(context, "messageWebSocketHandler", "MessageWebSocketHandler"); 
            checkBean(context, "webSocketConnectionManager", "WebSocketConnectionManager");
            
            // 检查配置属性Bean
            checkBean(context, "webSocketProperties", "WebSocketProperties");
            checkBean(context, "messageProperties", "MessageProperties");
            
            // 检查ObjectMapper Bean（按类型）
            try {
                var objectMapper = context.getBean(com.fasterxml.jackson.databind.ObjectMapper.class);
                log.info("✅ ObjectMapper Bean 加载成功: {}", objectMapper.getClass().getName());
            } catch (Exception e) {
                log.warn("⚠️ ObjectMapper Bean 按类型获取失败，尝试其他方式");
                checkBean(context, "objectMapper", "ObjectMapper");
            }
            
            // 统计WebSocket相关Bean数量
            String[] allBeans = context.getBeanDefinitionNames();
            long websocketBeanCount = java.util.Arrays.stream(allBeans)
                .filter(name -> name.toLowerCase().contains("websocket") || 
                               name.toLowerCase().contains("message"))
                .count();
                
            log.info("🔍 发现 {} 个WebSocket/Message相关Bean", websocketBeanCount);
            log.info("========== WebSocket Bean 检查完成 ==========");
        };
    }
    
    private void checkBean(ApplicationContext context, String beanName, String description) {
        try {
            var bean = context.getBean(beanName);
            log.info("✅ {} Bean 加载成功: {}", description, bean.getClass().getName());
        } catch (Exception e) {
            log.error("❌ {} Bean 加载失败: {}", description, e.getMessage());
        }
    }
}