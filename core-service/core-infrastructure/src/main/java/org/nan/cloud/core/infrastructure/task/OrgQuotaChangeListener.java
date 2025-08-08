package org.nan.cloud.core.infrastructure.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.event.quota.QuotaChangeEvent;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TaskDO;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.MaterialMapper;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.TaskMapper;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrgQuotaChangeListener {

    private final TaskMapper taskMapper;

    private final MaterialMapper materialMapper;

    @Async
    @EventListener
    public void handleQuotaChangeEvent(QuotaChangeEvent event) {
        QuotaChangeEvent.QuotaChangeEventType eventType = event.getEventType();
        if (Objects.requireNonNull(eventType) == QuotaChangeEvent.QuotaChangeEventType.MATERIAL_FILE_UPLOAD) {
            handleMaterialFileUploadEvent(event.getTaskId());
        }
    }

    private void handleMaterialFileUploadEvent(String taskId) {

        TaskDO taskDO = taskMapper.selectById(taskId);

        Long materialId = Long.parseLong(taskDO.getRefId());

        // todo: 一次性查出material\materialFile相关信息
        // 构建OrgQuotaChangeLogDocument并存储
        // 扣除组织空间



    }
}
