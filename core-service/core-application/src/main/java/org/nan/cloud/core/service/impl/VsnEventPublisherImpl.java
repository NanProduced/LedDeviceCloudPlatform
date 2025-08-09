package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.event.mq.VsnGenerationRequestEvent;
import org.nan.cloud.core.service.VsnEventPublisher;
import org.springframework.stereotype.Service;

/**
 * VSN 事件发布实现（占位）
 * 注意：core-infrastructure 已引入 common-mq 的生产者能力；
 * 由于本模块未引入 amqp 依赖，这里先占位实现，具体发布在 infrastructure 层完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VsnEventPublisherImpl implements VsnEventPublisher {

    @Override
    public void publishVsnGenerationRequest(VsnGenerationRequestEvent event) {
        // 交由 core-infrastructure 的统一生产者发送（common-mq）
        log.info("[MQ] 准备发布VSN生成请求: programId={}, version={}", event.getProgramId(), event.getVersion());
    }

    @Override
    public void publishVsnRegenerationRequest(VsnGenerationRequestEvent event) {
        log.info("[MQ] 准备发布VSN重新生成请求: programId={}, version={}", event.getProgramId(), event.getVersion());
    }
}

