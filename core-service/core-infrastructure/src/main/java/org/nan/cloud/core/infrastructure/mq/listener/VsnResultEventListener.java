package org.nan.cloud.core.infrastructure.mq.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.common.mq.consumer.ConsumeResult;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.core.event.mq.VsnGenerationResponseEvent;
import org.nan.cloud.core.repository.ProgramRepository;
import org.nan.cloud.program.enums.VsnGenerationStatusEnum;
import org.springframework.stereotype.Component;

/**
 * VSN生成结果监听器
 * 监听 business.core.queue 上来自 file-service 的 VSN 生成结果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VsnResultEventListener implements MessageConsumer {

    private final ProgramRepository programRepository;

    @Override
    public String[] getSupportedMessageTypes() {
        // 使用统一的 EVENT 类型，subject 区分；此处采用 Manager 的 supports 机制用 messageType
        // 若 file-service 采用 messageType = "EVENT"，此处返回空数组表示全量接收，再在 consume 中甄别 subject
        return new String[]{"EVENT"};
    }

    @Override
    public String getConsumerId() {
        return "VsnResultEventListener";
    }

    @Override
    public ConsumeResult consume(Message message) {
        long start = System.currentTimeMillis();
        try {
            // 仅处理 VSN 结果类 subject
            if (!"program.vsn.result".equalsIgnoreCase(message.getSubject())) {
                return ConsumeResult.success(message.getMessageId(), getConsumerId(), System.currentTimeMillis() - start);
            }

            VsnGenerationResponseEvent event = JsonUtils.getDefaultObjectMapper()
                    .convertValue(message.getPayload(), VsnGenerationResponseEvent.class);

            log.info("📥 收到VSN生成结果: programId={}, status={}, fileId={}",
                    event.getProgramId(), event.getStatus(), event.getVsnFileId());

            VsnGenerationStatusEnum status = event.getStatus();
            String fileId = event.getVsnFileId();
            String filePath = event.getVsnFilePath();
            String error = event.getErrorMessage();

            programRepository.updateVsnGenerationResult(
                    event.getProgramId(), status, fileId, filePath, error, 0L);

            return ConsumeResult.success(message.getMessageId(), getConsumerId(), System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("❌ 处理VSN生成结果失败: id={}, err={}", message.getMessageId(), e.getMessage(), e);
            return ConsumeResult.failure(message.getMessageId(), getConsumerId(), "VSN_RESULT_ERROR", e.getMessage(), e);
        }
    }
}

