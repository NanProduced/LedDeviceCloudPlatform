package org.nan.cloud.message.infrastructure.websocket.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.api.stomp.CommonStompMessage;
import org.nan.cloud.message.api.stomp.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.DispatchResult;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
import org.nan.cloud.message.infrastructure.websocket.processor.BusinessMessageProcessor;
import org.nan.cloud.message.infrastructure.websocket.sender.StompMessageSender;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TerminalStatusMessageProcessor implements BusinessMessageProcessor {

    private final StompMessageDispatcher stompMessageDispatcher;

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public String getSupportedMessageType() {
        return "TERMINAL_STATUS";
    }

    @Override
    public boolean supports(String messageType, String routingKey) {
        if (messageType == null || routingKey == null) {
            return false;
        }

        boolean typeSupport = "TERMINAL_STATUS".equals(messageType);
        boolean routingKeySupport = routingKey.startsWith("stomp.device.status");

        return typeSupport && routingKeySupport;
    }

    @Override
    public BusinessMessageProcessResult process(String messagePayload, String routingKey) {
        try {
            log.debug("开始处理终端状态消息 - 路由键: {}", routingKey);

            Map<String, Object> payload = JsonUtils.fromJson(messagePayload, Map.class);
            String processType = (String) payload.get("type");
            switch (processType) {
                case "ONLINE_STATUS":
                    return processTerminalOnlineStatusChange(payload, routingKey);
                case "LED_STATUS":
                    return processLedStatusReport(payload, routingKey);
                default:
                    throw new BaseException(ExceptionEnum.UNKNOWN_MQ_MESSAGE_TYPE, "invalid message type");
            }

        } catch (Exception e) {
            String errorMsg = String.format("终端上下线消息处理异常 - 路由键: %s, 错误: %s",
                    routingKey, e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }

    /**
     * 处理终端在线离线状态变更
     * @param payload
     * @param routingKey
     * @return
     */
    private BusinessMessageProcessResult processTerminalOnlineStatusChange(Map<String, Object> payload, String routingKey) {

        String tid =  (String) payload.get("tid");
        String status = (String) payload.get("subType");
        String timestamp = (String) payload.get("timestamp");
        Long oid = null;
        String[] parts = routingKey.split("\\.");               // ["stomp","device","status","123","456"]
        if (parts.length >= 2) {
            String secondLast = parts[parts.length - 2];   // "123"
            oid = Long.parseLong(secondLast);
        }

        CommonStompMessage stompMessage = CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(Instant.now().toString())
                .oid(oid)
                .messageType(StompMessageTypes.TERMINAL_STATUS)
                .subType_1(status)
                .context(CommonStompMessage.Context.terminalContext(Long.valueOf(tid)))
                .payload(Map.of("timestamp", timestamp))
                .priority(Priority.NORMAL)
                .build();

        DispatchResult result = stompMessageDispatcher.smartDispatch(stompMessage);

        return BusinessMessageProcessResult.success(stompMessage.getMessageId(), result, stompMessage);

    }

    /**
     * 处理终端led-status数据上报
     * @param payload
     * @param routingKey
     * @return
     */
    private BusinessMessageProcessResult processLedStatusReport(Map<String, Object> payload, String routingKey) {
        String oid =  (String) payload.get("oid");
        String tid = (String) payload.get("tid");
        String report = (String) payload.get("report");

        CommonStompMessage stompMessage = CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(Instant.now().toString())
                .oid(Long.valueOf(oid))
                .messageType(StompMessageTypes.TERMINAL_STATUS)
                .subType_1("LED_STATUS")
                .context(CommonStompMessage.Context.terminalContext(Long.valueOf(tid)))
                .payload(Map.of("report", report))
                .priority(Priority.NORMAL)
                .build();

        DispatchResult result = stompMessageDispatcher.smartDispatch(stompMessage);

        return BusinessMessageProcessResult.success(stompMessage.getMessageId(), result, stompMessage);
    }
}
