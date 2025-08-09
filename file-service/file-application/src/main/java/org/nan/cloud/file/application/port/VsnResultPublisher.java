package org.nan.cloud.file.application.port;

/**
 * VSN 结果发布端口（由基础设施层实现）
 */
public interface VsnResultPublisher {
    void publishResultCompleted(Long orgId, Long programId, Integer version,
                                String vsnFileId, String vsnFilePath, String thumbnailPath);

    void publishResultFailed(Long orgId, Long programId, Integer version,
                             String errorMessage, String errorDetails);
}

