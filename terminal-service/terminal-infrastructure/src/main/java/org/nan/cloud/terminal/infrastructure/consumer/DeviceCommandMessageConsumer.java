package org.nan.cloud.terminal.infrastructure.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.consumer.ConsumeResult;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.terminal.api.dto.message.DeviceCommandMessage;
import org.nan.cloud.terminal.infrastructure.connection.ShardedConnectionManager;
import org.nan.cloud.terminal.infrastructure.persistence.mongodb.document.OfflineMessageDocument;
import org.nan.cloud.terminal.infrastructure.persistence.mongodb.repository.OfflineMessageRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 设备指令消息消费者
 * 
 * 消费来自core-service的设备指令消息，根据设备在线状态：
 * 1. 设备在线：直接通过WebSocket推送指令
 * 2. 设备离线：将消息持久化到MongoDB，待设备上线后推送
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceCommandMessageConsumer implements MessageConsumer {
    
    private final ShardedConnectionManager connectionManager;
    private final OfflineMessageRepository offlineMessageRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 支持的消息类型
     */
    private static final String[] SUPPORTED_MESSAGE_TYPES = {
        "DEVICE_COMMAND"
    };
    
    @Override
    public ConsumeResult consume(Message message) {
        try {
            log.info("开始处理设备指令消息: {}", message.getDescription());
            
            // 解析消息内容
            DeviceCommandMessage commandMessage = parseCommandMessage(message);
            if (commandMessage == null) {
                return ConsumeResult.failure(message.getMessageId(), getConsumerId(), 
                    "PARSE_ERROR", "无法解析设备指令消息内容", null);
            }
            
            // 处理设备指令
            boolean sent = processDeviceCommand(commandMessage);
            
            if (sent) {
                log.info("设备指令已成功发送到在线设备: did={}", commandMessage.getDid());
                return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0L);
            } else {
                log.info("设备离线，消息已持久化: did={}", commandMessage.getDid());
                return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0L);
            }
            
        } catch (Exception e) {
            log.error("处理设备指令消息失败: {}", message.getDescription(), e);
            return ConsumeResult.failure(message.getMessageId(), getConsumerId(), 
                "PROCESS_ERROR", "处理设备指令消息异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理设备指令
     * 
     * @param commandMessage 指令消息
     * @return true表示已发送到在线设备，false表示设备离线已持久化
     */
    private boolean processDeviceCommand(DeviceCommandMessage commandMessage) {
        String did = commandMessage.getDid();
        
        // 检查设备是否在线
        if (connectionManager.isDeviceOnline(did)) {
            // 设备在线，直接发送消息
            return connectionManager.sendMessage(did, commandMessage.getContent());
        } else {
            // 设备离线，持久化消息
            persistOfflineMessage(commandMessage);
            return false;
        }
    }
    
    /**
     * 持久化离线消息
     * 
     * @param commandMessage 指令消息
     */
    private void persistOfflineMessage(DeviceCommandMessage commandMessage) {
        try {
            // 构建离线消息文档
            OfflineMessageDocument offlineMessage = OfflineMessageDocument.builder()
                .messageId(java.util.UUID.randomUUID().toString())
                .did(commandMessage.getDid())
                .oid(commandMessage.getOid())
                .messageType("DEVICE_COMMAND")
                .messageContent(commandMessage.getContent())
                .priority(5)
                .status(OfflineMessageDocument.MessageStatus.PENDING)
                .retryCount(0)
                .maxRetryCount(3)
                .createTime(LocalDateTime.now())
                .expireTime(LocalDateTime.now().plusHours(24)) // 24小时过期
                .build();
            
            // 保存到MongoDB
            offlineMessageRepository.save(offlineMessage);
            
            log.debug("离线消息已持久化: did={}, messageId={}", 
                commandMessage.getDid(), offlineMessage.getMessageId());
                
        } catch (Exception e) {
            log.error("持久化离线消息失败: did={}", commandMessage.getDid(), e);
            throw new RuntimeException("持久化离线消息失败", e);
        }
    }
    
    /**
     * 解析指令消息
     * 
     * @param message MQ消息
     * @return 设备指令消息对象
     */
    private DeviceCommandMessage parseCommandMessage(Message message) {
        try {
            if (message.getPayload() instanceof DeviceCommandMessage) {
                return (DeviceCommandMessage) message.getPayload();
            } else if (message.getPayload() instanceof String) {
                return objectMapper.readValue((String) message.getPayload(), DeviceCommandMessage.class);
            } else {
                String json = objectMapper.writeValueAsString(message.getPayload());
                return objectMapper.readValue(json, DeviceCommandMessage.class);
            }
        } catch (Exception e) {
            log.error("解析设备指令消息失败: {}", message.getDescription(), e);
            return null;
        }
    }
    
    @Override
    public String[] getSupportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }
    
    @Override
    public String getConsumerId() {
        return "DeviceCommandMessageConsumer";
    }
}