package org.nan.cloud.terminal.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.SendResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminalOnlineMessageService {

    private final MessageProducer messageProducer;

    private static final String MESSAGE_TYPE = "TERMINAL_STATUS";
    private static final String EXCHANGE_NAME = "stomp.push.topic";
    private static final String ROUTING_KEY_TEMPLATE = "stomp.device.status.%d.%d";

    public void sendTerminalOnline(Long oid, Long tid, String timestamp) {

        try {
            Message message = Message.builder()
                    .messageType(MESSAGE_TYPE)
                    .subject("终端上线")
                    .senderId("terminal-service")
                    .receiverId(tid.toString())
                    .organizationId(oid.toString())
                    .exchange(EXCHANGE_NAME)
                    .routingKey(String.format(ROUTING_KEY_TEMPLATE, oid, tid))
                    .priority(7)
                    .sourceSystem("terminal-service")
                    .targetSystem("message-service")
                    .payload(buildPayload("ONLINE", tid, timestamp))
                    .build();

            SendResult result = messageProducer.send(message);
            if (result.isSuccess()) {
                log.info("✅ 终端上线消息发送完成 - oid: {}, tid: {}, messageId: {}",
                        oid, tid, result.getMessageId());
            } else {
                log.error("❌ 终端上线消息发送失败 - oid: {}, tid: {}, 错误: {}",
                        oid, tid, result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("发送终端上线消息异常 - oid: {}, tid: {}, 错误: {}",
                    oid, tid, e.getMessage(), e);
        }
    }

    public  void sendTerminalOffline(Long oid, Long tid, String timestamp) {
        try {
            Message message = Message.builder()
                    .messageType(MESSAGE_TYPE)
                    .subject("终端下线")
                    .senderId("terminal-service")
                    .receiverId(tid.toString())
                    .organizationId(oid.toString())
                    .exchange(EXCHANGE_NAME)
                    .routingKey(String.format(ROUTING_KEY_TEMPLATE, oid, tid))
                    .priority(7)
                    .sourceSystem("terminal-service")
                    .targetSystem("message-service")
                    .payload(buildPayload("OFFLINE", tid, timestamp))
                    .build();

            SendResult result = messageProducer.send(message);
            if (result.isSuccess()) {
                log.info("✅ 终端下线消息发送完成 - oid: {}, tid: {}, messageId: {}",
                        oid, tid, result.getMessageId());
            } else {
                log.error("❌ 终端下线消息发送失败 - oid: {}, tid: {}, 错误: {}",
                        oid, tid, result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("发送终端下线消息异常 - oid: {}, tid: {}, 错误: {}",
                    oid, tid, e.getMessage(), e);
        }

    }

    private Map<String, Object> buildPayload(String type, Long tid, String timestamp) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ONLINE_STATUS");
        payload.put("subType", type);
        payload.put("tid", tid.toString());
        payload.put("timestamp", timestamp);
        return payload;
    }
}
