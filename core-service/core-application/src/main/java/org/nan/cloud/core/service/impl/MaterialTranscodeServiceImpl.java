package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.enums.TaskTypeEnum;
import org.nan.cloud.core.service.MaterialTranscodeService;
import org.nan.cloud.core.service.TaskService;
import org.nan.cloud.core.service.TranscodeEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialTranscodeServiceImpl implements MaterialTranscodeService {

    private final TaskService taskService;
    private final TranscodeEventPublisher transcodeEventPublisher;

    @Override
    public String submitTranscode(Long materialId, Long oid, Long ugid, Long uid) {
        String taskId = UUID.randomUUID().toString();
        taskService.createTask(Task.builder()
                .taskId(taskId)
                .taskType(TaskTypeEnum.MATERIAL_TRANSCODE)
                .taskStatus(TaskStatusEnum.PENDING)
                .oid(oid)
                .ref("material:" + materialId)
                .refId(String.valueOf(materialId))
                .creator(uid)
                .progress(0)
                .createTime(LocalDateTime.now())
                .build());
        log.info("✅ 素材转码任务已提交 - taskId:{} - uid:{}", taskId, uid);
        transcodeEventPublisher.publishTranscodeTask(materialId, taskId, oid, ugid, uid);
        return taskId;
    }
}
