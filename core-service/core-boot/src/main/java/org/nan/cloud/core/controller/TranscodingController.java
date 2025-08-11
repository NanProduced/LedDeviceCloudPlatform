package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.api.TranscodingApi;
import org.nan.cloud.core.api.DTO.req.TranscodingTaskQueryRequest;
import org.nan.cloud.core.api.DTO.res.TranscodingTaskResponse;
import org.nan.cloud.core.service.TranscodingTaskService;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 转码任务查询控制器
 */
@Tag(name = "Transcoding(转码任务查询)", description = "转码任务查询相关操作")
@RestController
@RequiredArgsConstructor
@Slf4j
public class TranscodingController implements TranscodingApi {

    private final TranscodingTaskService transcodingTaskService;

    @Override
    public TranscodingTaskResponse queryTranscodingTasks(@RequestBody @Valid TranscodingTaskQueryRequest request) {
        RequestUserInfo user = InvocationContextHolder.getContext().getRequestUser();
        log.info("🔍 查询转码任务 - uid={}, request={}", user.getUid(), request);
        
        return transcodingTaskService.queryUserTranscodingTasks(user.getUid(), user.getOid(), request);
    }

    @Override
    public TranscodingTaskResponse.TranscodingTaskInfo getTranscodingTaskDetail(@PathVariable String taskId) {
        RequestUserInfo user = InvocationContextHolder.getContext().getRequestUser();
        log.info("🔍 获取转码任务详情 - uid={}, taskId={}", user.getUid(), taskId);
        
        return transcodingTaskService.getTranscodingTaskDetail(taskId, user.getUid(), user.getOid());
    }

    @Override
    public TranscodingTaskResponse getTranscodingTasksBySource(@PathVariable Long sourceMaterialId) {
        RequestUserInfo user = InvocationContextHolder.getContext().getRequestUser();
        log.info("🔍 根据源素材查询转码任务 - uid={}, sourceMaterialId={}", user.getUid(), sourceMaterialId);
        
        return transcodingTaskService.getTranscodingTasksBySource(sourceMaterialId, user.getUid(), user.getOid());
    }
}