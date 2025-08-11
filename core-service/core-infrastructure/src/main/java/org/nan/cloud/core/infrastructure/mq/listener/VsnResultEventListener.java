package org.nan.cloud.core.infrastructure.mq.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.MaterialMetadata;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.common.mq.consumer.ConsumeResult;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.core.domain.ProgramMaterialRef;
import org.nan.cloud.core.event.mq.VsnGenerationResponseEvent;
import org.nan.cloud.core.repository.MaterialMetadataRepository;
import org.nan.cloud.core.repository.MaterialRepository;
import org.nan.cloud.core.repository.ProgramMaterialRefRepository;
import org.nan.cloud.core.repository.ProgramRepository;
import org.nan.cloud.program.enums.VsnGenerationStatusEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;

/**
 * VSN生成结果监听器
 * 监听 business.core.queue 上来自 file-service 的 VSN 生成结果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VsnResultEventListener implements MessageConsumer {

    private final ProgramRepository programRepository;
    private final ProgramMaterialRefRepository programMaterialRefRepository;
    private final MaterialMetadataRepository materialMetadataRepository;
    private final MaterialRepository materialRepository;

    @Override
    public String[] getSupportedMessageTypes() {
        // 使用统一的 EVENT 类型，subject 区分；此处采用 Manager 的 supports 机制用 messageType
        // 若 file-service 采用 messageType = "EVENT"，此处返回空数组表示全量接收，再在 consume 中甄别 subject
        return new String[]{"EVENT"};
    }

    @Override
    public String getConsumerId() {
        return "VsnResultEventListener";
    }

    @Override
    public ConsumeResult consume(Message message) {
        long start = System.currentTimeMillis();
        try {
            // 仅处理 VSN 结果类 subject
            if (!"program.vsn.result".equalsIgnoreCase(message.getSubject())) {
                return ConsumeResult.success(message.getMessageId(), getConsumerId(), System.currentTimeMillis() - start);
            }

            VsnGenerationResponseEvent event = JsonUtils.getDefaultObjectMapper()
                    .convertValue(message.getPayload(), VsnGenerationResponseEvent.class);

            log.info("📥 收到VSN生成结果: programId={}, status={}, fileId={}",
                    event.getProgramId(), event.getStatus(), event.getVsnFileId());

            VsnGenerationStatusEnum status = event.getStatus();
            String fileId = event.getVsnFileId();
            String filePath = event.getVsnFilePath();
            String error = event.getErrorMessage();

            programRepository.updateVsnGenerationResult(
                    event.getProgramId(), status, fileId, filePath, error, 0L);
            
            // 如果VSN生成成功，更新节目缩略图为第一个素材的缩略图
            if (VsnGenerationStatusEnum.COMPLETED.equals(status)) {
                try {
                    updateProgramThumbnail(event.getProgramId());
                } catch (Exception e) {
                    log.warn("⚠️ 更新节目缩略图失败: programId={}, error={}", 
                            event.getProgramId(), e.getMessage(), e);
                    // 缩略图更新失败不影响VSN结果处理
                }
            }

            return ConsumeResult.success(message.getMessageId(), getConsumerId(), System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("❌ 处理VSN生成结果失败: id={}, err={}", message.getMessageId(), e.getMessage(), e);
            return ConsumeResult.failure(message.getMessageId(), getConsumerId(), "VSN_RESULT_ERROR", e.getMessage(), e);
        }
    }
    
    /**
     * 更新节目缩略图
     * 使用节目中第一个素材的主缩略图作为节目缩略图
     * @param programId 节目ID
     */
    private void updateProgramThumbnail(Long programId) {
        log.debug("🖼️ 开始更新节目缩略图: programId={}", programId);
        
        // 获取节目的素材引用列表
        List<ProgramMaterialRef> materialRefs = programMaterialRefRepository.findByProgramId(programId);
        if (CollectionUtils.isEmpty(materialRefs)) {
            log.debug("节目无素材引用，跳过缩略图更新: programId={}", programId);
            return;
        }
        
        // 按顺序排序，获取第一个素材
        // 先按序号排序，再按ID排序确保一致性
        materialRefs.sort(Comparator.comparingInt(ProgramMaterialRef::getUsageIndex).thenComparingLong(ProgramMaterialRef::getMaterialId));
        
        ProgramMaterialRef firstMaterial = materialRefs.get(0);
        Long firstMaterialId = firstMaterial.getMaterialId();
        String firstMaterialFileId = materialRepository.getFileIdByMaterialId(firstMaterialId);

        log.debug("获取第一个素材: materialId={}, fileId={}",
                firstMaterial.getMaterialId(), firstMaterialFileId);

        // 获取素材的元数据
        MaterialMetadata metadata = materialMetadataRepository.findByFileId(firstMaterialFileId);
        if (metadata == null) {
            log.debug("素材元数据不存在，跳过缩略图更新: fileId={}", firstMaterialFileId);
            return;
        }

        // 获取主缩略图URL
        String thumbnailUrl = null;
        if (metadata.getThumbnails() != null &&
            metadata.getThumbnails().getPrimaryThumbnail() != null) {
            thumbnailUrl = metadata.getThumbnails().getPrimaryThumbnail().getStorageUrl();
        }

        if (thumbnailUrl == null) {
            log.debug("素材无缩略图，跳过缩略图更新: fileId={}", firstMaterialFileId);
            return;
        }
        
        // 更新节目缩略图
        int updatedRows = programRepository.updateThumbnailUrl(programId, thumbnailUrl, 0L);
        if (updatedRows > 0) {
            log.info("✅ 成功更新节目缩略图: programId={}, thumbnailUrl={}", programId, thumbnailUrl);
        } else {
            log.warn("⚠️ 节目缩略图更新无影响: programId={}, thumbnailUrl={}", programId, thumbnailUrl);
        }
    }
}

