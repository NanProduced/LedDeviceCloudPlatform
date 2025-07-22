package org.nan.cloud.message.infrastructure.mq.consumer;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;
import org.nan.cloud.message.api.enums.MessageType;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.api.event.MessageEvent;
import org.nan.cloud.message.infrastructure.redis.manager.MessageCacheManager;
import org.nan.cloud.message.infrastructure.websocket.manager.WebSocketConnectionManager;
import org.nan.cloud.message.utils.MessageUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 系统监控消费者
 * 
 * 处理系统监控和性能相关的事件消息，包括队列监控、性能指标收集、系统告警等。
 * 负责监控整个消息系统的健康状态，确保系统稳定运行。
 * 
 * 主要处理的监控事件类型：
 * 1. QUEUE_HEALTH_CHECK - 队列健康检查事件
 * 2. PERFORMANCE_METRICS - 性能指标收集事件
 * 3. SYSTEM_ALERT - 系统告警事件
 * 4. RESOURCE_USAGE - 资源使用情况事件
 * 5. CONNECTION_STATUS - 连接状态监控事件
 * 6. ERROR_STATISTICS - 错误统计事件
 * 
 * 监控策略：
 * - 实时监控队列状态和性能指标
 * - 收集和分析错误统计信息
 * - 自动触发告警和通知
 * - 生成监控报告和趋势分析
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMonitoringConsumer {
    
    private final WebSocketConnectionManager webSocketConnectionManager;
    private final MessageCacheManager messageCacheManager;
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 处理系统通知队列
     * 
     * 消费system.notification.queue中的系统级通知事件。
     * 
     * @param event 系统通知事件
     * @param message RabbitMQ原始消息
     * @param channel RabbitMQ通道
     */
    @RabbitListener(
        queues = "system.notification.queue",
        ackMode = "MANUAL",
        concurrency = "1-3"
    )
    public void handleSystemNotification(MessageEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("处理系统通知事件: messageId={}, eventType={}", 
                    event.getMessageId(), event.getEventType());
            
            // 根据系统事件类型进行不同处理
            switch (event.getEventType()) {
                case "QUEUE_HEALTH_CHECK":
                    handleQueueHealthCheckEvent(event);
                    break;
                case "PERFORMANCE_METRICS":
                    handlePerformanceMetricsEvent(event);
                    break;
                case "SYSTEM_ALERT":
                    handleSystemAlertEvent(event);
                    break;
                case "RESOURCE_USAGE":
                    handleResourceUsageEvent(event);
                    break;
                case "CONNECTION_STATUS":
                    handleConnectionStatusEvent(event);
                    break;
                case "ERROR_STATISTICS":
                    handleErrorStatisticsEvent(event);
                    break;
                default:
                    log.warn("未知的系统事件类型: eventType={}, messageId={}", 
                            event.getEventType(), event.getMessageId());
                    handleUnknownSystemEvent(event);
            }
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("系统通知事件处理完成: messageId={}, eventType={}", 
                    event.getMessageId(), event.getEventType());
                    
        } catch (Exception e) {
            log.error("处理系统通知事件失败: messageId={}, eventType={}, error={}", 
                     event.getMessageId(), event.getEventType(), e.getMessage(), e);
            
            handleSystemEventProcessingError(event, channel, deliveryTag, e);
        }
    }
    
    /**
     * 处理队列健康检查事件
     * 
     * 监控各个队列的健康状态，包括队列长度、消费速率等指标。
     * 
     * @param event 队列健康检查事件
     */
    private void handleQueueHealthCheckEvent(MessageEvent event) {
        try {
            Map<String, Object> healthInfo = event.getMetadata();
            String queueName = (String) healthInfo.get("queueName");
            Integer queueLength = (Integer) healthInfo.get("queueLength");
            Integer consumerCount = (Integer) healthInfo.get("consumerCount");
            Double consumeRate = (Double) healthInfo.get("consumeRate");
            String healthStatus = (String) healthInfo.get("healthStatus");
            
            log.info("处理队列健康检查: queue={}, length={}, consumers={}, rate={}, status={}", 
                    queueName, queueLength, consumerCount, consumeRate, healthStatus);
            
            // 记录队列健康指标到Redis
            recordQueueHealthMetrics(queueName, queueLength, consumerCount, consumeRate, healthStatus);
            
            // 如果队列状态异常，发送告警
            if ("UNHEALTHY".equals(healthStatus) || "WARNING".equals(healthStatus)) {
                sendQueueHealthAlert(queueName, queueLength, consumerCount, healthStatus);
            }
            
        } catch (Exception e) {
            log.error("处理队列健康检查事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理性能指标事件
     * 
     * 收集和分析系统性能指标，如吞吐量、延迟、错误率等。
     * 
     * @param event 性能指标事件
     */
    private void handlePerformanceMetricsEvent(MessageEvent event) {
        try {
            Map<String, Object> metricsInfo = event.getMetadata();
            String metricType = (String) metricsInfo.get("metricType");
            Double metricValue = (Double) metricsInfo.get("metricValue");
            String metricUnit = (String) metricsInfo.get("metricUnit");
            String componentName = (String) metricsInfo.get("componentName");
            
            log.info("处理性能指标事件: component={}, type={}, value={}, unit={}", 
                    componentName, metricType, metricValue, metricUnit);
            
            // 记录性能指标到Redis
            recordPerformanceMetrics(componentName, metricType, metricValue, metricUnit);
            
            // 检查是否超过阈值
            if (isMetricAboveThreshold(metricType, metricValue)) {
                sendPerformanceAlert(componentName, metricType, metricValue, metricUnit);
            }
            
        } catch (Exception e) {
            log.error("处理性能指标事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理系统告警事件
     * 
     * 处理系统级别的重要告警，需要立即通知运维人员。
     * 
     * @param event 系统告警事件
     */
    private void handleSystemAlertEvent(MessageEvent event) {
        try {
            Map<String, Object> alertInfo = event.getMetadata();
            String alertLevel = (String) alertInfo.get("alertLevel");
            String alertSource = (String) alertInfo.get("alertSource");
            String alertMessage = (String) alertInfo.get("alertMessage");
            String alertCode = (String) alertInfo.get("alertCode");
            
            log.error("处理系统告警事件: level={}, source={}, code={}, message={}", 
                     alertLevel, alertSource, alertCode, alertMessage);
            
            // 记录告警到Redis
            recordSystemAlert(alertLevel, alertSource, alertCode, alertMessage);
            
            // 创建系统告警通知消息
            WebSocketMessage systemAlert = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SYSTEM_ALERT)
                .title(String.format("系统告警 - %s", alertLevel))
                .content(String.format("告警来源：%s，告警代码：%s，详细信息：%s", 
                        alertSource, alertCode, alertMessage))
                .priority(determineAlertPriority(alertLevel))
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "alertLevel", alertLevel,
                    "alertSource", alertSource != null ? alertSource : "",
                    "alertMessage", alertMessage != null ? alertMessage : "",
                    "alertCode", alertCode != null ? alertCode : "",
                    "eventType", "SYSTEM_ALERT",
                    "alertTime", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
                ))
                .requireAck(true)
                .build();
            
            // 广播给所有管理员
            int sentCount = webSocketConnectionManager.broadcastToAll(systemAlert);
            
            log.error("系统告警通知已发送: level={}, source={}, sentCount={}", 
                     alertLevel, alertSource, sentCount);
            
        } catch (Exception e) {
            log.error("处理系统告警事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理资源使用情况事件
     * 
     * 监控系统资源使用情况，如CPU、内存、磁盘等。
     * 
     * @param event 资源使用情况事件
     */
    private void handleResourceUsageEvent(MessageEvent event) {
        try {
            Map<String, Object> resourceInfo = event.getMetadata();
            String resourceType = (String) resourceInfo.get("resourceType");
            Double usagePercentage = (Double) resourceInfo.get("usagePercentage");
            String hostName = (String) resourceInfo.get("hostName");
            Long totalAmount = (Long) resourceInfo.get("totalAmount");
            Long usedAmount = (Long) resourceInfo.get("usedAmount");
            
            log.info("处理资源使用情况事件: host={}, resource={}, usage={}%, used={}, total={}", 
                    hostName, resourceType, usagePercentage, usedAmount, totalAmount);
            
            // 记录资源使用指标到Redis
            recordResourceUsageMetrics(hostName, resourceType, usagePercentage, usedAmount, totalAmount);
            
            // 检查资源使用率是否过高
            if (usagePercentage != null && usagePercentage > 80.0) {
                sendResourceUsageAlert(hostName, resourceType, usagePercentage);
            }
            
        } catch (Exception e) {
            log.error("处理资源使用情况事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理连接状态事件
     * 
     * 监控各种连接的状态，如数据库连接、Redis连接、WebSocket连接等。
     * 
     * @param event 连接状态事件
     */
    private void handleConnectionStatusEvent(MessageEvent event) {
        try {
            Map<String, Object> connectionInfo = event.getMetadata();
            String connectionType = (String) connectionInfo.get("connectionType");
            String connectionName = (String) connectionInfo.get("connectionName");
            String status = (String) connectionInfo.get("status");
            Integer activeConnections = (Integer) connectionInfo.get("activeConnections");
            Integer maxConnections = (Integer) connectionInfo.get("maxConnections");
            
            log.info("处理连接状态事件: type={}, name={}, status={}, active={}, max={}", 
                    connectionType, connectionName, status, activeConnections, maxConnections);
            
            // 记录连接状态到Redis
            recordConnectionStatusMetrics(connectionType, connectionName, status, activeConnections, maxConnections);
            
            // 如果连接状态异常，发送告警
            if ("DISCONNECTED".equals(status) || "ERROR".equals(status)) {
                sendConnectionStatusAlert(connectionType, connectionName, status);
            }
            
        } catch (Exception e) {
            log.error("处理连接状态事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理错误统计事件
     * 
     * 收集和分析系统错误统计信息，用于系统健康监控。
     * 
     * @param event 错误统计事件
     */
    private void handleErrorStatisticsEvent(MessageEvent event) {
        try {
            Map<String, Object> errorInfo = event.getMetadata();
            String errorType = (String) errorInfo.get("errorType");
            String componentName = (String) errorInfo.get("componentName");
            Integer errorCount = (Integer) errorInfo.get("errorCount");
            String timeWindow = (String) errorInfo.get("timeWindow");
            
            log.info("处理错误统计事件: component={}, type={}, count={}, window={}", 
                    componentName, errorType, errorCount, timeWindow);
            
            // 记录错误统计到Redis
            recordErrorStatistics(componentName, errorType, errorCount, timeWindow);
            
            // 检查错误率是否过高
            if (errorCount != null && errorCount > 50) { // 假设阈值为50
                sendErrorRateAlert(componentName, errorType, errorCount, timeWindow);
            }
            
        } catch (Exception e) {
            log.error("处理错误统计事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理未知系统事件
     * 
     * 对于未识别的系统事件类型，进行通用处理。
     * 
     * @param event 未知系统事件
     */
    private void handleUnknownSystemEvent(MessageEvent event) {
        try {
            log.warn("处理未知系统事件: eventType={}, messageId={}", 
                    event.getEventType(), event.getMessageId());
            
            // 记录未知事件用于后续分析
            String unknownEventKey = "unknown_system_events:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            messageCacheManager.incrementCounter(unknownEventKey, 1, 24 * 60 * 60);
            
        } catch (Exception e) {
            log.error("处理未知系统事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 记录队列健康指标到Redis
     */
    private void recordQueueHealthMetrics(String queueName, Integer queueLength, 
                                        Integer consumerCount, Double consumeRate, String healthStatus) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
            String baseKey = "queue_health:" + queueName + ":" + today;
            
            if (queueLength != null) {
                messageCacheManager.cacheData(baseKey + ":length", queueLength.toString(), 24 * 60 * 60);
            }
            if (consumerCount != null) {
                messageCacheManager.cacheData(baseKey + ":consumers", consumerCount.toString(), 24 * 60 * 60);
            }
            if (consumeRate != null) {
                messageCacheManager.cacheData(baseKey + ":rate", consumeRate.toString(), 24 * 60 * 60);
            }
            messageCacheManager.cacheData(baseKey + ":status", healthStatus, 24 * 60 * 60);
            
        } catch (Exception e) {
            log.error("记录队列健康指标失败: queue={}, error={}", queueName, e.getMessage());
        }
    }
    
    /**
     * 发送队列健康告警
     */
    private void sendQueueHealthAlert(String queueName, Integer queueLength, 
                                    Integer consumerCount, String healthStatus) {
        try {
            WebSocketMessage healthAlert = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SYSTEM_ALERT)
                .title("队列健康告警")
                .content(String.format("队列 %s 健康状态异常：%s。队列长度：%d，消费者数量：%d。", 
                        queueName, healthStatus, queueLength, consumerCount))
                .priority(Priority.HIGH)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "queueName", queueName,
                    "queueLength", queueLength.toString(),
                    "consumerCount", consumerCount.toString(),
                    "healthStatus", healthStatus,
                    "alertType", "QUEUE_HEALTH"
                ))
                .requireAck(true)
                .build();
            
            int sentCount = webSocketConnectionManager.broadcastToAll(healthAlert);
            log.warn("队列健康告警已发送: queue={}, status={}, sentCount={}", 
                    queueName, healthStatus, sentCount);
            
        } catch (Exception e) {
            log.error("发送队列健康告警失败: queue={}, error={}", queueName, e.getMessage());
        }
    }
    
    /**
     * 记录性能指标到Redis
     */
    private void recordPerformanceMetrics(String componentName, String metricType, 
                                        Double metricValue, String metricUnit) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
            String metricsKey = "performance_metrics:" + componentName + ":" + metricType + ":" + timestamp;
            
            messageCacheManager.cacheData(metricsKey, metricValue.toString(), 24 * 60 * 60);
            
        } catch (Exception e) {
            log.error("记录性能指标失败: component={}, metric={}, error={}", 
                     componentName, metricType, e.getMessage());
        }
    }
    
    /**
     * 检查指标是否超过阈值
     */
    private boolean isMetricAboveThreshold(String metricType, Double metricValue) {
        // 简单的阈值检查逻辑
        switch (metricType) {
            case "CPU_USAGE":
                return metricValue > 80.0;
            case "MEMORY_USAGE":
                return metricValue > 85.0;
            case "RESPONSE_TIME":
                return metricValue > 5000.0; // 5秒
            case "ERROR_RATE":
                return metricValue > 5.0; // 5%
            default:
                return false;
        }
    }
    
    /**
     * 发送性能告警
     */
    private void sendPerformanceAlert(String componentName, String metricType, 
                                    Double metricValue, String metricUnit) {
        try {
            WebSocketMessage perfAlert = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SYSTEM_ALERT)
                .title("性能告警")
                .content(String.format("组件 %s 的 %s 指标异常：%s %s，已超过预设阈值。", 
                        componentName, metricType, metricValue, metricUnit))
                .priority(Priority.HIGH)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "componentName", componentName,
                    "metricType", metricType,
                    "metricValue", metricValue.toString(),
                    "metricUnit", metricUnit,
                    "alertType", "PERFORMANCE"
                ))
                .requireAck(true)
                .build();
            
            int sentCount = webSocketConnectionManager.broadcastToAll(perfAlert);
            log.warn("性能告警已发送: component={}, metric={}, value={}, sentCount={}", 
                    componentName, metricType, metricValue, sentCount);
            
        } catch (Exception e) {
            log.error("发送性能告警失败: component={}, error={}", componentName, e.getMessage());
        }
    }
    
    /**
     * 记录系统告警到Redis
     */
    private void recordSystemAlert(String alertLevel, String alertSource, 
                                 String alertCode, String alertMessage) {
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String alertKey = "system_alerts:" + alertLevel + ":" + System.currentTimeMillis();
            
            String alertData = String.format("{\"level\":\"%s\",\"source\":\"%s\",\"code\":\"%s\",\"message\":\"%s\",\"time\":\"%s\"}", 
                    alertLevel, alertSource, alertCode, alertMessage, timestamp);
            
            messageCacheManager.cacheData(alertKey, alertData, 7 * 24 * 60 * 60); // 保存7天
            
        } catch (Exception e) {
            log.error("记录系统告警失败: level={}, error={}", alertLevel, e.getMessage());
        }
    }
    
    /**
     * 记录资源使用指标
     */
    private void recordResourceUsageMetrics(String hostName, String resourceType, 
                                          Double usagePercentage, Long usedAmount, Long totalAmount) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
            String resourceKey = "resource_usage:" + hostName + ":" + resourceType + ":" + timestamp;
            
            messageCacheManager.cacheData(resourceKey + ":percentage", usagePercentage.toString(), 24 * 60 * 60);
            if (usedAmount != null) {
                messageCacheManager.cacheData(resourceKey + ":used", usedAmount.toString(), 24 * 60 * 60);
            }
            if (totalAmount != null) {
                messageCacheManager.cacheData(resourceKey + ":total", totalAmount.toString(), 24 * 60 * 60);
            }
            
        } catch (Exception e) {
            log.error("记录资源使用指标失败: host={}, resource={}, error={}", 
                     hostName, resourceType, e.getMessage());
        }
    }
    
    /**
     * 发送资源使用告警
     */
    private void sendResourceUsageAlert(String hostName, String resourceType, Double usagePercentage) {
        try {
            WebSocketMessage resourceAlert = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SYSTEM_ALERT)
                .title("资源使用告警")
                .content(String.format("主机 %s 的 %s 使用率达到 %.1f%%，请及时处理。", 
                        hostName, resourceType, usagePercentage))
                .priority(Priority.HIGH)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "hostName", hostName,
                    "resourceType", resourceType,
                    "usagePercentage", usagePercentage.toString(),
                    "alertType", "RESOURCE_USAGE"
                ))
                .requireAck(true)
                .build();
            
            int sentCount = webSocketConnectionManager.broadcastToAll(resourceAlert);
            log.warn("资源使用告警已发送: host={}, resource={}, usage={}%, sentCount={}", 
                    hostName, resourceType, usagePercentage, sentCount);
            
        } catch (Exception e) {
            log.error("发送资源使用告警失败: host={}, error={}", hostName, e.getMessage());
        }
    }
    
    /**
     * 记录连接状态指标
     */
    private void recordConnectionStatusMetrics(String connectionType, String connectionName, 
                                             String status, Integer activeConnections, Integer maxConnections) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
            String connKey = "connection_status:" + connectionType + ":" + connectionName + ":" + timestamp;
            
            messageCacheManager.cacheData(connKey + ":status", status, 24 * 60 * 60);
            if (activeConnections != null) {
                messageCacheManager.cacheData(connKey + ":active", activeConnections.toString(), 24 * 60 * 60);
            }
            if (maxConnections != null) {
                messageCacheManager.cacheData(connKey + ":max", maxConnections.toString(), 24 * 60 * 60);
            }
            
        } catch (Exception e) {
            log.error("记录连接状态指标失败: type={}, name={}, error={}", 
                     connectionType, connectionName, e.getMessage());
        }
    }
    
    /**
     * 发送连接状态告警
     */
    private void sendConnectionStatusAlert(String connectionType, String connectionName, String status) {
        try {
            WebSocketMessage connectionAlert = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SYSTEM_ALERT)
                .title("连接状态告警")
                .content(String.format("连接 %s (%s) 状态异常：%s，请检查连接配置。", 
                        connectionName, connectionType, status))
                .priority(Priority.HIGH)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "connectionType", connectionType,
                    "connectionName", connectionName,
                    "status", status,
                    "alertType", "CONNECTION_STATUS"
                ))
                .requireAck(true)
                .build();
            
            int sentCount = webSocketConnectionManager.broadcastToAll(connectionAlert);
            log.warn("连接状态告警已发送: type={}, name={}, status={}, sentCount={}", 
                    connectionType, connectionName, status, sentCount);
            
        } catch (Exception e) {
            log.error("发送连接状态告警失败: type={}, name={}, error={}", 
                     connectionType, connectionName, e.getMessage());
        }
    }
    
    /**
     * 记录错误统计信息
     */
    private void recordErrorStatistics(String componentName, String errorType, 
                                     Integer errorCount, String timeWindow) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
            String errorKey = "error_stats:" + componentName + ":" + errorType + ":" + timestamp;
            
            messageCacheManager.cacheData(errorKey, errorCount.toString(), 24 * 60 * 60);
            
        } catch (Exception e) {
            log.error("记录错误统计失败: component={}, type={}, error={}", 
                     componentName, errorType, e.getMessage());
        }
    }
    
    /**
     * 发送错误率告警
     */
    private void sendErrorRateAlert(String componentName, String errorType, 
                                  Integer errorCount, String timeWindow) {
        try {
            WebSocketMessage errorAlert = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SYSTEM_ALERT)
                .title("错误率告警")
                .content(String.format("组件 %s 在 %s 时间窗口内发生 %d 次 %s 类型错误，错误率过高。", 
                        componentName, timeWindow, errorCount, errorType))
                .priority(Priority.HIGH)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "componentName", componentName,
                    "errorType", errorType,
                    "errorCount", errorCount.toString(),
                    "timeWindow", timeWindow,
                    "alertType", "ERROR_RATE"
                ))
                .requireAck(true)
                .build();
            
            int sentCount = webSocketConnectionManager.broadcastToAll(errorAlert);
            log.warn("错误率告警已发送: component={}, type={}, count={}, sentCount={}", 
                    componentName, errorType, errorCount, sentCount);
            
        } catch (Exception e) {
            log.error("发送错误率告警失败: component={}, error={}", componentName, e.getMessage());
        }
    }
    
    /**
     * 根据告警级别确定消息优先级
     */
    private Priority determineAlertPriority(String alertLevel) {
        if (alertLevel == null) {
            return Priority.NORMAL;
        }
        
        switch (alertLevel.toUpperCase()) {
            case "CRITICAL":
            case "FATAL":
                return Priority.URGENT;
            case "HIGH":
            case "ERROR":
                return Priority.HIGH;
            case "LOW":
            case "INFO":
                return Priority.LOW;
            case "MEDIUM":
            case "WARNING":
            default:
                return Priority.NORMAL;
        }
    }
    
    /**
     * 处理系统事件处理错误
     */
    private void handleSystemEventProcessingError(MessageEvent event, Channel channel, 
                                                long deliveryTag, Exception exception) {
        try {
            // 增加重试次数
            event.incrementRetry();
            event.setErrorMessage(exception.getMessage());
            event.setProcessedTime(LocalDateTime.now());
            
            if (event.canRetry()) {
                log.warn("系统事件处理失败，消息重新入队: messageId={}, retryCount={}, error={}", 
                        event.getMessageId(), event.getRetryCount(), exception.getMessage());
                // 拒绝消息，重新入队
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("系统事件处理失败，超过最大重试次数，消息进入死信队列: messageId={}, retryCount={}, error={}", 
                        event.getMessageId(), event.getRetryCount(), exception.getMessage());
                // 拒绝消息，不重新入队，进入死信队列
                channel.basicNack(deliveryTag, false, false);
            }
            
        } catch (IOException e) {
            log.error("处理系统事件错误失败: messageId={}, error={}", event.getMessageId(), e.getMessage());
        }
    }
}