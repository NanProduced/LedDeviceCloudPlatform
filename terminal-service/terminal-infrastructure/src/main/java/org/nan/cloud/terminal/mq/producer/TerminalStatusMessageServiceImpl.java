package org.nan.cloud.terminal.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.SendResult;
import org.nan.cloud.terminal.application.domain.TerminalStatusReport;
import org.nan.cloud.terminal.application.handler.TerminalStatusMessageService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminalStatusMessageServiceImpl implements TerminalStatusMessageService {

    private final MessageProducer messageProducer;

    private static final String EXCHANGE_NAME = "stomp.push.topic";
    private static final String ROUTING_KEY_TEMPLATE = "stomp.device.status.%d.%d";

    @Override
    public void sendTerminalStatusMessage(Long oid, Long tid, TerminalStatusReport report) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "LED_STATUS");
        payload.put("oid", oid);
        payload.put("tid", tid);
        payload.put("report", report);

        Message message = Message.builder()
                .messageType("TERMINAL_STATUS")
                .subject("终端信息上报")
                .payload(payload)
                .senderId(tid.toString())
                .receiverId(tid.toString())
                .organizationId(oid.toString())
                .exchange(EXCHANGE_NAME)
                .routingKey(String.format(ROUTING_KEY_TEMPLATE, oid, tid))
                .priority(5) // 一般优先级
                .sourceSystem("terminal-service")
                .targetSystem("message-service")
                .build();

        SendResult result = messageProducer.send(message);
        if (result.isSuccess()) {
            log.debug("✅ 终端上报状态消息发送完成 - oid: {}, tid: {}, messageId: {}",
                    oid, tid, result.getMessageId());
        } else {
            log.error("❌ 终端上报状态消息发送失败 - oid: {}, tid: {}, 错误: {}",
                    oid, tid, result.getErrorMessage());
        }
    }
}
