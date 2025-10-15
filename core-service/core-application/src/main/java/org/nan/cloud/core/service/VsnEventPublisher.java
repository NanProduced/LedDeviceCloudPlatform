package org.nan.cloud.core.service;

import org.nan.cloud.core.event.mq.VsnGenerationRequestEvent;

/**
 * VSN事件发布服务接口
 * 负责发布VSN相关的消息队列事件
 */
public interface VsnEventPublisher {
    
    /**
     * 发布VSN生成请求事件
     * @param event VSN生成请求事件
     */
    void publishVsnGenerationRequest(VsnGenerationRequestEvent event);
    
    /**
     * 发布VSN重新生成请求事件
     * @param event VSN生成请求事件
     */
    void publishVsnRegenerationRequest(VsnGenerationRequestEvent event);
}