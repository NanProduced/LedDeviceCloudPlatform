package org.nan.cloud.core.infrastructure.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.event.quota.QuotaChangeEvent;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TaskDO;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.MaterialMapper;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.TaskMapper;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.OrgQuotaMapper;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.OrgQuotaDO;
import org.nan.cloud.core.infrastructure.repository.mongo.document.OrgQuotaChangeLogDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    private final OrgQuotaMapper orgQuotaMapper;

    private final MongoTemplate mongoTemplate;

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
        if (taskDO == null) {
            log.warn("Task not found, skip quota change. taskId={}", taskId);
            return;
        }

        Long materialId = null;
        try {
            materialId = Long.parseLong(taskDO.getRefId());
        } catch (Exception e) {
            log.warn("Invalid refId for taskId={}, refId={}", taskId, taskDO.getRefId());
        }

        // 使用连表查询一次性拿到文件字段（MaterialDO 扩展了非持久化字段）
        org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialDO materialDO =
                materialId != null ? materialMapper.selectMaterialWithFileById(materialId)
                                   : materialMapper.selectMaterialWithFileByFileId(taskDO.getRefId());

        if (materialDO == null) {
            log.warn("Material not found for taskId={}, refId={}.", taskId, taskDO.getRefId());
            return;
        }

        Long oid = materialDO.getOid();
        Long ugid = materialDO.getUgid();
        Long fid = materialDO.getFid();
        String fileId = materialDO.getFileId();
        Long bytes = materialDO.getOriginalFileSize() == null ? 0L : materialDO.getOriginalFileSize();

        // 读取组织当前配额
        OrgQuotaDO quota = orgQuotaMapper.selectById(oid);
        long beforeUsedSize = quota != null && quota.getUsedStorageSize() != null ? quota.getUsedStorageSize() : 0L;
        int beforeUsedCount = quota != null && quota.getUsedFileCount() != null ? quota.getUsedFileCount() : 0;

        long afterUsedSize = beforeUsedSize + bytes;
        int afterUsedCount = beforeUsedCount + 1;

        // 更新配额（简单加法；低并发场景，接受轻微竞态）
        OrgQuotaDO update = new OrgQuotaDO();
        update.setOrgId(oid);
        update.setUsedStorageSize(afterUsedSize);
        update.setUsedFileCount(afterUsedCount);
        orgQuotaMapper.updateById(update);

        // 记录 Mongo 日志
        OrgQuotaChangeLogDocument logDoc = new OrgQuotaChangeLogDocument();
        logDoc.setOid(oid);
        logDoc.setFileId(fileId);
        logDoc.setUgid(ugid);
        logDoc.setFid(fid);
        logDoc.setFileType("MATERIAL");
        logDoc.setSourceId(materialId != null ? materialId.toString() : null);
        logDoc.setBytesChange(bytes);
        logDoc.setFilesChange(1);
        logDoc.setQuotaOperationType(org.nan.cloud.core.enums.QuotaOperationType.MATERIAL_UPLOAD);
        logDoc.setBeforeUsedSize(beforeUsedSize);
        logDoc.setAfterUsedSize(afterUsedSize);
        logDoc.setBeforeUsedCount(beforeUsedCount);
        logDoc.setAfterUsedCount(afterUsedCount);
        logDoc.setTaskId(taskId);
        logDoc.setRemark("material upload settlement");
        logDoc.setCreatedAt(java.time.LocalDateTime.now());
        mongoTemplate.save(logDoc);
    }
}
