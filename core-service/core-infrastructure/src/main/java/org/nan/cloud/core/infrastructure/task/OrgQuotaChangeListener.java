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
import org.nan.cloud.core.repository.ProgramRepository;
import org.nan.cloud.core.domain.Program;
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
    
    private final ProgramRepository programRepository;

    @Async
    @EventListener
    public void handleQuotaChangeEvent(QuotaChangeEvent event) {
        QuotaChangeEvent.QuotaChangeEventType eventType = event.getEventType();
        switch (Objects.requireNonNull(eventType)) {
            case MATERIAL_FILE_UPLOAD:
                handleMaterialFileUploadEvent(event.getTaskId());
                break;
            case VSN_CREATE:
                handleVsnCreateEvent(event.getTaskId());
                break;
            case VSN_DELETE:
                handleVsnDeleteEvent(event.getTaskId());
                break;
            default:
                log.debug("未处理的配额变更事件类型: {}", eventType);
                break;
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

    /**
     * 处理VSN创建事件
     * @param programId 节目ID（作为字符串传递）
     */
    private void handleVsnCreateEvent(String programId) {
        log.info("VSN文件创建成功 - 开始扣除组织存储空间");
        try {
            Long id = Long.valueOf(programId);
            Program program = programRepository.findById(id).orElse(null);
            if (program == null) {
                log.warn("节目不存在，跳过配额扣除. programId={}", programId);
                return;
            }
            
            if (program.getVsnFileSize() == null || program.getVsnFileSize() <= 0) {
                log.warn("VSN文件大小为空或无效，跳过配额扣除. programId={}, size={}", 
                        programId, program.getVsnFileSize());
                return;
            }

            // 读取组织当前配额
            OrgQuotaDO quota = orgQuotaMapper.selectById(program.getOid());
            if (quota == null) {
                log.warn("组织配额不存在，跳过配额扣除. orgId={}", program.getOid());
                return;
            }
            
            long beforeUsedSize = quota.getUsedStorageSize() != null ? quota.getUsedStorageSize() : 0L;
            int beforeUsedCount = quota.getUsedFileCount() != null ? quota.getUsedFileCount() : 0;
            
            long afterUsedSize = beforeUsedSize + program.getVsnFileSize();
            int afterUsedCount = beforeUsedCount + 1;

            // 生成日志
            OrgQuotaChangeLogDocument logDoc = buildVsnCreateLog(quota, program, beforeUsedSize, beforeUsedCount);

            // 更新配额（简单加法；低并发场景，接受轻微竞态）
            OrgQuotaDO update = new OrgQuotaDO();
            update.setOrgId(program.getOid());
            update.setUsedStorageSize(afterUsedSize);
            update.setUsedFileCount(afterUsedCount);
            orgQuotaMapper.updateById(update);

            log.info("VSN文件创建成功 - 扣除组织存储空间成功: 组织:{}, 本次扣除:{} MB, 文件数量:{}", 
                    program.getOid(), program.getVsnFileSize() / 1024 / 1024, 1);

            // 记录 Mongo 日志
            mongoTemplate.save(logDoc);
            
        } catch (Exception e) {
            log.error("处理VSN创建配额事件失败: programId={}, error={}", programId, e.getMessage(), e);
        }
    }

    /**
     * 处理VSN删除事件
     * @param programId 节目ID（作为字符串传递）
     */
    private void handleVsnDeleteEvent(String programId) {
        log.info("VSN文件删除 - 开始回收组织存储空间");
        try {
            Long id = Long.valueOf(programId);
            Program program = programRepository.findById(id).orElse(null);
            if (program == null) {
                log.warn("节目不存在，跳过配额回收. programId={}", programId);
                return;
            }
            
            if (program.getVsnFileSize() == null || program.getVsnFileSize() <= 0) {
                log.debug("VSN文件大小为空，跳过配额回收. programId={}", programId);
                return;
            }

            // 读取组织当前配额
            OrgQuotaDO quota = orgQuotaMapper.selectById(program.getOid());
            if (quota == null) {
                log.warn("组织配额不存在，跳过配额回收. orgId={}", program.getOid());
                return;
            }
            
            long beforeUsedSize = quota.getUsedStorageSize() != null ? quota.getUsedStorageSize() : 0L;
            int beforeUsedCount = quota.getUsedFileCount() != null ? quota.getUsedFileCount() : 0;
            
            long afterUsedSize = Math.max(0, beforeUsedSize - program.getVsnFileSize());
            int afterUsedCount = Math.max(0, beforeUsedCount - 1);

            // 生成日志
            OrgQuotaChangeLogDocument logDoc = buildVsnDeleteLog(quota, program, beforeUsedSize, beforeUsedCount);

            // 更新配额
            OrgQuotaDO update = new OrgQuotaDO();
            update.setOrgId(program.getOid());
            update.setUsedStorageSize(afterUsedSize);
            update.setUsedFileCount(afterUsedCount);
            orgQuotaMapper.updateById(update);

            log.info("VSN文件删除 - 回收组织存储空间成功: 组织:{}, 本次回收:{} MB, 文件数量:{}", 
                    program.getOid(), program.getVsnFileSize() / 1024 / 1024, 1);

            // 记录 Mongo 日志
            mongoTemplate.save(logDoc);
            
        } catch (Exception e) {
            log.error("处理VSN删除配额事件失败: programId={}, error={}", programId, e.getMessage(), e);
        }
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

    private OrgQuotaChangeLogDocument buildVsnCreateLog(OrgQuotaDO orgQuotaDO, Program program, 
                                                       long beforeUsedSize, int beforeUsedCount) {
        return OrgQuotaChangeLogDocument.builder()
                .oid(program.getOid())
                .fileId(program.getVsnFileId())
                .ugid(program.getUgid())
                .fid(null) // VSN文件不关联文件夹
                .fileType("VSN")
                .sourceId(program.getId().toString())
                .bytesChange(program.getVsnFileSize()) // 正数表示增加
                .filesChange(1)
                .quotaOperationType(QuotaOperationType.VSN_UPLOAD)
                .beforeUsedSize(beforeUsedSize)
                .afterUsedSize(beforeUsedSize + program.getVsnFileSize())
                .beforeUsedCount(beforeUsedCount)
                .afterUsedCount(beforeUsedCount + 1)
                .operationUid(program.getCreatedBy())
                .taskId(null) // VSN生成没有taskId
                .remark("节目VSN文件生成")
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    private OrgQuotaChangeLogDocument buildVsnDeleteLog(OrgQuotaDO orgQuotaDO, Program program, 
                                                       long beforeUsedSize, int beforeUsedCount) {
        return OrgQuotaChangeLogDocument.builder()
                .oid(program.getOid())
                .fileId(program.getVsnFileId())
                .ugid(program.getUgid())
                .fid(null) // VSN文件不关联文件夹
                .fileType("VSN")
                .sourceId(program.getId().toString())
                .bytesChange(-program.getVsnFileSize()) // 负数表示减少
                .filesChange(-1)
                .quotaOperationType(QuotaOperationType.VSN_DELETE)
                .beforeUsedSize(beforeUsedSize)
                .afterUsedSize(Math.max(0, beforeUsedSize - program.getVsnFileSize()))
                .beforeUsedCount(beforeUsedCount)
                .afterUsedCount(Math.max(0, beforeUsedCount - 1))
                .operationUid(program.getUpdatedBy())
                .taskId(null) // VSN删除没有taskId
                .remark("节目VSN文件删除")
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }
}
