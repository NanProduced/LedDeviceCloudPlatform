package org.nan.cloud.file.application.service;

import java.util.Map;

/**
 * VSN 生成服务（严格模式）
 */
public interface VsnGenerationService {
    void generate(Map<String, Object> requestPayload);
}

