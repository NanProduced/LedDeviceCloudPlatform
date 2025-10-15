package org.nan.cloud.core.service;

public interface TranscodeEventPublisher {

    /**
     * 向mq推送转码任务
     * @param materialId
     * @param taskId
     * @param oid
     * @param Ugid
     * @param uid
     */
    void publishTranscodeTask(Long materialId, String taskId, Long oid, Long Ugid, Long uid);
}
