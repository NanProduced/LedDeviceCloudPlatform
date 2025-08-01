package org.nan.cloud.terminal.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.SendResult;
import org.nan.cloud.terminal.api.common.model.TerminalCommand;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 指令确认消息发送服务
 * 
 * 负责将设备的指令确认结果发送到消息队列，供message-service进行STOMP推送。
 * 
 * 功能职责：
 * 1. 构建标准化的指令确认消息格式
 * 2. 发送消息到指定的MQ交换机和路由键
 * 3. 提供同步和异步发送选项
 * 4. 错误处理和重试机制
 * 
 * 消息格式设计：
 * - 消息类型：COMMAND_RESULT
 * - 路由键：stomp.command.result.{orgId}.{tid}
 * - 交换机：stomp.push.topic
 * - 消息内容：指令ID、设备信息、执行结果、时间戳等
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandConfirmationMessageService {
    
    private final MessageProducer messageProducer;
    
    // 消息常量
    private static final String MESSAGE_TYPE = "COMMAND_RESULT";
    private static final String EXCHANGE_NAME = "stomp.push.topic";
    private static final String ROUTING_KEY_TEMPLATE = "stomp.command.result.%d.%d";
    
    /**
     * 发送指令执行成功消息
     * 
     * @param oid 组织ID
     * @param tid 终端ID
     * @param commandId 指令ID
     * @param command 原始指令对象
     * @param userId 发送指令的用户ID（从指令中获取）
     */
    public void sendCommandExecutionSuccess(Long oid, Long tid, Integer commandId, 
                                          TerminalCommand command, Long userId) {
        try {
            Map<String, Object> payload = buildSuccessPayload(oid, tid, commandId, command, userId);
            
            Message message = Message.builder()
                    .messageType(MESSAGE_TYPE)
                    .subject("指令执行成功")
                    .payload(payload)
                    .senderId("terminal-service")
                    .receiverId(userId.toString())
                    .organizationId(oid.toString())
                    .exchange(EXCHANGE_NAME)
                    .routingKey(String.format(ROUTING_KEY_TEMPLATE, oid, tid))
                    .priority(7) // 高优先级，及时通知用户
                    .sourceSystem("terminal-service")
                    .targetSystem("message-service")
                    .build();
            
            SendResult result = messageProducer.send(message);
            
            if (result.isSuccess()) {
                log.info("✅ 指令执行成功消息发送完成 - oid: {}, tid: {}, commandId: {}, userId: {}, messageId: {}", 
                        oid, tid, commandId, userId, result.getMessageId());
            } else {
                log.error("❌ 指令执行成功消息发送失败 - oid: {}, tid: {}, commandId: {}, 错误: {}", 
                        oid, tid, commandId, result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("发送指令执行成功消息异常 - oid: {}, tid: {}, commandId: {}, 错误: {}", 
                    oid, tid, commandId, e.getMessage(), e);
        }
    }
    
    /**
     * 发送指令被拒绝消息
     * 
     * @param oid 组织ID
     * @param tid 终端ID
     * @param commandId 指令ID
     * @param command 原始指令对象
     * @param userId 发送指令的用户ID
     * @param rejectionReason 拒绝原因
     */
    public void sendCommandRejection(Long oid, Long tid, Integer commandId, 
                                   TerminalCommand command, Long userId, String rejectionReason) {
        try {
            Map<String, Object> payload = buildRejectionPayload(oid, tid, commandId, command, 
                    userId, rejectionReason);
            
            Message message = Message.builder()
                    .messageType(MESSAGE_TYPE)
                    .subject("指令被拒绝")
                    .payload(payload)
                    .senderId("terminal-service")
                    .receiverId(userId.toString())
                    .organizationId(oid.toString())
                    .exchange(EXCHANGE_NAME)
                    .routingKey(String.format(ROUTING_KEY_TEMPLATE, oid, tid))
                    .priority(7) // 高优先级，及时通知用户
                    .sourceSystem("terminal-service")
                    .targetSystem("message-service")
                    .build();
            
            SendResult result = messageProducer.send(message);
            
            if (result.isSuccess()) {
                log.info("✅ 指令拒绝消息发送完成 - oid: {}, tid: {}, commandId: {}, userId: {}, reason: {}, messageId: {}", 
                        oid, tid, commandId, userId, rejectionReason, result.getMessageId());
            } else {
                log.error("❌ 指令拒绝消息发送失败 - oid: {}, tid: {}, commandId: {}, 错误: {}", 
                        oid, tid, commandId, result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("发送指令拒绝消息异常 - oid: {}, tid: {}, commandId: {}, 错误: {}", 
                    oid, tid, commandId, e.getMessage(), e);
        }
    }
    
    /**
     * 构建成功执行的消息载荷
     */
    private Map<String, Object> buildSuccessPayload(Long oid, Long tid, Integer commandId, 
                                                   TerminalCommand command, Long userId) {
        Map<String, Object> payload = new HashMap<>();
        
        // 基础信息
        payload.put("commandId", commandId.toString());
        payload.put("deviceId", tid.toString());
        payload.put("terminalId", tid);
        payload.put("orgId", oid);
        payload.put("userId", userId);
        payload.put("result", "success");
        
        // 执行结果
        payload.put("executionStatus", "SUCCESS");
        payload.put("executionResult", "Executable comment");
        payload.put("message", "指令执行成功");
        payload.put("confirmTime", LocalDateTime.now().toString());
        
        return payload;
    }
    
    /**
     * 构建拒绝执行的消息载荷
     */
    private Map<String, Object> buildRejectionPayload(Long oid, Long tid, Integer commandId, 
                                                     TerminalCommand command, Long userId, String rejectionReason) {
        Map<String, Object> payload = new HashMap<>();
        
        // 基础信息
        payload.put("commandId", commandId);
        payload.put("deviceId", tid.toString());
        payload.put("terminalId", tid);
        payload.put("orgId", oid);
        payload.put("userId", userId);
        payload.put("result", "failed");
        
        // 执行结果
        payload.put("executionStatus", "REJECTED");
        payload.put("message", "指令被设备拒绝执行");
        payload.put("rejectionReason", rejectionReason);
        
        // 指令详情
        if (command != null) {
            payload.put("originalCommand", Map.of(
                    "content", command.getContent(),
                    "authorUrl", command.getAuthorUrl()
            ));
        }
        
        // 时间信息
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("confirmTime", LocalDateTime.now().toString());
        
        return payload;
    }
    
    /**
     * 异步发送指令执行成功消息
     */
    public void sendCommandExecutionSuccessAsync(Long oid, Long tid, Integer commandId, 
                                               TerminalCommand command, Long userId) {
        // 可以在这里实现异步发送逻辑
        // 目前先使用同步发送，后续可以优化为真正的异步
        sendCommandExecutionSuccess(oid, tid, commandId, command, userId);
    }
    
    /**
     * 异步发送指令拒绝消息
     */
    public void sendCommandRejectionAsync(Long oid, Long tid, Integer commandId, 
                                        TerminalCommand command, Long userId, String rejectionReason) {
        // 可以在这里实现异步发送逻辑
        // 目前先使用同步发送，后续可以优化为真正的异步
        sendCommandRejection(oid, tid, commandId, command, userId, rejectionReason);
    }
}