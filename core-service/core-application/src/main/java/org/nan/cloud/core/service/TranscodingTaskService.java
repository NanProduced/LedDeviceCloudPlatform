package org.nan.cloud.core.service;

import org.nan.cloud.core.api.DTO.req.TranscodingTaskQueryRequest;
import org.nan.cloud.core.api.DTO.res.TranscodingTaskResponse;

/**
 * 转码任务查询服务接口
 */
public interface TranscodingTaskService {

    /**
     * 查询用户转码任务列表
     * @param uid 用户ID
     * @param oid 组织ID
     * @param request 查询请求
     * @return 转码任务响应
     */
    TranscodingTaskResponse queryUserTranscodingTasks(Long uid, Long oid, TranscodingTaskQueryRequest request);

    /**
     * 获取转码任务详情
     * @param taskId 任务ID
     * @param uid 用户ID（权限控制）
     * @param oid 组织ID（权限控制）
     * @return 转码任务详情
     */
    TranscodingTaskResponse.TranscodingTaskInfo getTranscodingTaskDetail(String taskId, Long uid, Long oid);

    /**
     * 根据源素材ID查询转码任务
     * @param sourceMaterialId 源素材ID
     * @param uid 用户ID（权限控制）
     * @param oid 组织ID（权限控制）
     * @return 转码任务列表
     */
    TranscodingTaskResponse getTranscodingTasksBySource(Long sourceMaterialId, Long uid, Long oid);
}