package org.nan.cloud.core.infrastructure.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.enums.QuotaOperationType;
import org.nan.cloud.core.event.quota.QuotaChangeEvent;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialDO;
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

    /**
     * 处理素材文件上传事件
     * @param taskId
     */
    private void handleMaterialFileUploadEvent(String taskId) {
        log.info("素材文件上传成功 - 开始扣除组织存储空间");
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
        MaterialDO materialDO = materialMapper.selectMaterialWithFileById(materialId);

        if (materialDO == null) {
            log.warn("Material not found for taskId={}, refId={}.", taskId, taskDO.getRefId());
            return;
        }

        Long bytes = materialDO.getOriginalFileSize() == null ? 0L : materialDO.getOriginalFileSize();

        // 读取组织当前配额
        OrgQuotaDO quota = orgQuotaMapper.selectById(materialDO.getOid());
        long beforeUsedSize = quota != null && quota.getUsedStorageSize() != null ? quota.getUsedStorageSize() : 0L;
        int beforeUsedCount = quota != null && quota.getUsedFileCount() != null ? quota.getUsedFileCount() : 0;

        long afterUsedSize = beforeUsedSize + bytes;
        int afterUsedCount = beforeUsedCount + 1;

        // 生成日志
        OrgQuotaChangeLogDocument logDoc = buildMaterialUploadLog(quota, materialDO, taskDO);

        // 更新配额（简单加法；低并发场景，接受轻微竞态）
        OrgQuotaDO update = new OrgQuotaDO();
        update.setOrgId(materialDO.getOid());
        update.setUsedStorageSize(afterUsedSize);
        update.setUsedFileCount(afterUsedCount);
        orgQuotaMapper.updateById(update);

        log.info("素材文件上传成功 - 扣除组织存储空间成功: 组织:{}, 本次扣除:{} Mib, 文件数量:{}", materialDO.getOid(), bytes, 1);

        // 记录 Mongo 日志
        mongoTemplate.save(logDoc);
    }

    /* ----------------- 构建空间更改日志辅助方法 ----------------------- */

    private OrgQuotaChangeLogDocument buildMaterialUploadLog(OrgQuotaDO orgQuotaDO, MaterialDO materialDO, TaskDO taskDO) {
        // 减素材文件大小
        return OrgQuotaChangeLogDocument.builder()
                .oid(materialDO.getOid())
                .fileId(materialDO.getFileId())
                .ugid(materialDO.getUgid())
                .fid(materialDO.getFid())
                .fileType("MATERIAL")
                .sourceId(materialDO.getMid().toString())
                .bytesChange(-materialDO.getOriginalFileSize()) // 减素材文件大小
                .filesChange(-1)
                .quotaOperationType(QuotaOperationType.MATERIAL_UPLOAD)
                .beforeUsedSize(orgQuotaDO.getUsedStorageSize())
                .afterUsedSize(orgQuotaDO.getUsedStorageSize() + materialDO.getOriginalFileSize())
                .beforeUsedCount(orgQuotaDO.getUsedFileCount())
                .afterUsedCount(orgQuotaDO.getUsedFileCount() + 1)
                .operationUid(taskDO.getCreator())
                .taskId(taskDO.getTaskId())
                .createdAt(taskDO.getCompleteTime())
                .build();
    }
}
