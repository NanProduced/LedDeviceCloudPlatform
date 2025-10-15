package org.nan.cloud.file.infrastructure.mq.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.consumer.ConsumeResult;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.file.application.service.VsnGenerationService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VsnGenerateRequestListener implements MessageConsumer {

    private final VsnGenerationService vsnGenerationService;

    @Override
    public String[] getSupportedMessageTypes() {
        return new String[]{"EVENT"};
    }

    @Override
    public String getConsumerId() { return "VsnGenerateRequestListener"; }

    @Override
    @SuppressWarnings("unchecked")
    public ConsumeResult consume(Message message) {
        long start = System.currentTimeMillis();
        try {
            if (!"program.vsn.generate".equalsIgnoreCase(message.getSubject())) {
                return ConsumeResult.success(message.getMessageId(), getConsumerId(), System.currentTimeMillis() - start);
            }
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            vsnGenerationService.generate(payload);
            return ConsumeResult.success(message.getMessageId(), getConsumerId(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("❌ 处理VSN生成请求失败: id={}, err={}", message.getMessageId(), e.getMessage(), e);
            return ConsumeResult.failure(message.getMessageId(), getConsumerId(), "VSN_GENERATE_ERROR", e.getMessage(), e);
        }
    }
}

