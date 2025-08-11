package org.nan.cloud.core.service;

public interface MaterialTranscodeService {

    /**
     * 提交转码请求
     * @param materialId 转码的素材Id
     * @param oid
     * @param ugid
     * @param uid
     * @return 转码任务ID
     */
    String submitTranscode(Long materialId, Long oid, Long ugid, Long uid);
}
