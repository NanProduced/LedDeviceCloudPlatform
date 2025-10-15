package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.api.DTO.req.TranscodingTaskQueryRequest;
import org.nan.cloud.core.api.DTO.res.TranscodingTaskResponse;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.enums.TaskTypeEnum;
import org.nan.cloud.core.repository.TaskRepository;
import org.nan.cloud.core.repository.MaterialRepository;
import org.nan.cloud.core.service.TranscodingTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 转码任务查询服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodingTaskServiceImpl implements TranscodingTaskService {

    private final TaskRepository taskRepository;
    private final MaterialRepository materialRepository;
    // TODO: 需要添加TranscodingDetailRepository来查询转码详情

    @Override
    public TranscodingTaskResponse queryUserTranscodingTasks(Long uid, Long oid, TranscodingTaskQueryRequest request) {
        log.info("🔍 查询用户转码任务 - uid={}, oid={}", uid, oid);

        // 构建查询条件
        List<Task> tasks = taskRepository.findTranscodingTasksByUser(
                uid, oid, 
                TaskTypeEnum.MATERIAL_TRANSCODE,
                request.getStatus(),
                request.getStartTime(), 
                request.getEndTime(),
                request.getPage(),
                request.getSize(),
                request.getSortBy(),
                request.getSortDirection()
        );

        // 转换为DTO
        List<TranscodingTaskResponse.TranscodingTaskInfo> taskInfos = tasks.stream()
                .map(this::convertToTaskInfo)
                .collect(Collectors.toList());

        // 获取总数量
        Integer total = taskRepository.countTranscodingTasksByUser(
                uid, oid, 
                TaskTypeEnum.MATERIAL_TRANSCODE,
                request.getStatus(),
                request.getStartTime(),
                request.getEndTime()
        );

        return TranscodingTaskResponse.builder()
                .tasks(taskInfos)
                .total(total)
                .build();
    }

    @Override
    public TranscodingTaskResponse.TranscodingTaskInfo getTranscodingTaskDetail(String taskId, Long uid, Long oid) {
        log.info("🔍 获取转码任务详情 - taskId={}, uid={}", taskId, uid);

        // 验证权限：确保任务属于当前用户
        Task task = taskRepository.findTranscodingTaskByIdAndUser(taskId, uid, oid, TaskTypeEnum.MATERIAL_TRANSCODE);
        if (task == null) {
            log.warn("⚠️ 转码任务不存在或无权限访问 - taskId={}, uid={}", taskId, uid);
            return null;
        }

        return convertToTaskInfo(task);
    }

    @Override
    public TranscodingTaskResponse getTranscodingTasksBySource(Long sourceMaterialId, Long uid, Long oid) {
        log.info("🔍 根据源素材查询转码任务 - sourceMaterialId={}, uid={}", sourceMaterialId, uid);

        // 验证源素材是否属于当前用户
        Material sourceMaterial = materialRepository.findByIdAndUser(sourceMaterialId, uid, oid);
        if (sourceMaterial == null) {
            log.warn("⚠️ 源素材不存在或无权限访问 - materialId={}, uid={}", sourceMaterialId, uid);
            return TranscodingTaskResponse.builder()
                    .tasks(List.of())
                    .total(0)
                    .build();
        }

        // 查询相关的转码任务
        List<Task> tasks = taskRepository.findTranscodingTasksBySourceMaterial(
                sourceMaterialId, uid, oid, TaskTypeEnum.MATERIAL_TRANSCODE
        );

        List<TranscodingTaskResponse.TranscodingTaskInfo> taskInfos = tasks.stream()
                .map(this::convertToTaskInfo)
                .collect(Collectors.toList());

        return TranscodingTaskResponse.builder()
                .tasks(taskInfos)
                .total(taskInfos.size())
                .build();
    }

    /**
     * 转换Task为TranscodingTaskInfo
     */
    private TranscodingTaskResponse.TranscodingTaskInfo convertToTaskInfo(Task task) {
        // 获取源素材信息
        Long sourceMaterialId = extractSourceMaterialId(task.getRef());
        TranscodingTaskResponse.MaterialInfo sourceMaterial = null;
        TranscodingTaskResponse.MaterialInfo targetMaterial = null;

        if (sourceMaterialId != null) {
            Material source = materialRepository.findById(sourceMaterialId);
            if (source != null) {
                sourceMaterial = convertToMaterialInfo(source);

                // 查找转码后的素材（通过source_material_id关联）
                List<Material> targets = materialRepository.findBySourceMaterialId(sourceMaterialId);
                if (!targets.isEmpty()) {
                    // 取最新的转码结果
                    Material target = targets.get(0);
                    targetMaterial = convertToMaterialInfo(target);
                }
            }
        }

        // TODO: 获取转码详情信息
        TranscodingTaskResponse.TranscodingDetailInfo transcodingDetail = null;

        return TranscodingTaskResponse.TranscodingTaskInfo.builder()
                .taskId(task.getTaskId())
                .status(task.getTaskStatus().name())
                .progress(task.getProgress())
                .transcodePreset(extractTranscodePreset(targetMaterial))
                .sourceMaterial(sourceMaterial)
                .targetMaterial(targetMaterial)
                .transcodingDetail(transcodingDetail)
                .createTime(task.getCreateTime())
                .completeTime(task.getCompleteTime())
                .build();
    }

    /**
     * 转换Material为MaterialInfo
     */
    private TranscodingTaskResponse.MaterialInfo convertToMaterialInfo(Material material) {
        if (material == null) return null;

        return TranscodingTaskResponse.MaterialInfo.builder()
                .materialId(material.getMid())
                .materialName(material.getMaterialName())
                .fileId(material.getFileId())
                .fileSize(material.getOriginalFileSize())
                .mimeType(material.getMimeType())
                .fileExtension(material.getFileExtension())
                .storagePath(material.getStoragePath())
                .md5Hash(material.getMd5Hash())
                .createTime(material.getCreateTime())
                .build();
    }

    /**
     * 从任务ref中提取源素材ID
     * ref格式: "material:123"
     */
    private Long extractSourceMaterialId(String ref) {
        if (ref != null && ref.startsWith("material:")) {
            try {
                return Long.valueOf(ref.substring("material:".length()));
            } catch (NumberFormatException e) {
                log.warn("⚠️ 解析源素材ID失败 - ref={}", ref);
            }
        }
        return null;
    }

    /**
     * 提取转码预设名称
     */
    private String extractTranscodePreset(TranscodingTaskResponse.MaterialInfo targetMaterial) {
        // TODO: 从targetMaterial或TranscodingDetail中获取预设名称
        return "standard"; // 临时返回默认值
    }
}