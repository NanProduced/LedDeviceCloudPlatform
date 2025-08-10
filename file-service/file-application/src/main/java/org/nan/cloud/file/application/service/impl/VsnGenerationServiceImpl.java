package org.nan.cloud.file.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.service.VsnGenerationService;
import org.nan.cloud.file.application.port.VsnResultPublisher;
import org.nan.cloud.file.application.repository.ProgramContentRepository;
import org.nan.cloud.file.application.service.validation.VsnSchemaValidator;
import org.nan.cloud.file.application.port.VsnXmlWriter;
import org.nan.cloud.file.application.port.StreamingVsnXmlWriter;
import org.nan.cloud.file.application.utils.StreamingHashCalculator;
import org.nan.cloud.file.application.utils.MemoryMonitor;
import org.nan.cloud.program.document.ProgramContent;
import org.nan.cloud.program.document.VsnProgram;
import org.nan.cloud.file.application.config.FileStorageProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.io.FileOutputStream;

/**
 * 严格模式 VSN 生成服务实现（第一阶段：管道搭建+基本校验占位）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VsnGenerationServiceImpl implements VsnGenerationService {

    private final VsnResultPublisher vsnResultPublisher;
    private final ProgramContentRepository programContentRepository;
    private final FileStorageProperties fileStorageProperties;
    private final VsnSchemaValidator vsnSchemaValidator;
    private final VsnXmlWriter vsnXmlWriter;
    private final StreamingVsnXmlWriter streamingVsnXmlWriter;

    @Override
    public void generate(Map<String, Object> requestPayload) {
        MemoryMonitor.MemoryWatcher watcher = null;
        try {
            Long orgId = toLong(requestPayload.get("organizationId"));
            Long programId = toLong(requestPayload.get("programId"));
            Integer version = toInt(requestPayload.get("version"));
            String contentId = String.valueOf(requestPayload.get("contentId"));
            String programName = requestPayload.get("programName") == null ? null : String.valueOf(requestPayload.get("programName"));

            watcher = new MemoryMonitor.MemoryWatcher("VSN生成-程序" + programId);
            MemoryMonitor.logMemoryUsage("VSN生成开始");

            // 1) 从 Mongo 读取 ProgramContent（按 contentId）
            ProgramContent content = programContentRepository.findById(contentId);
            watcher.checkpoint("读取程序内容");

            // 2) 校验 ProgramContent → VsnProgram 列表存在性与基本结构
            List<VsnProgram> vsnPrograms = content.getVsnPrograms();
            if (vsnPrograms == null || vsnPrograms.isEmpty()) {
                throw new IllegalArgumentException("vsnPrograms 为空，无法生成VSN");
            }
            vsnSchemaValidator.validate(vsnPrograms);
            watcher.checkpoint("模式校验完成");

            // 3) 使用流式处理生成VSN文件，同时计算MD5和文件大小
            StreamingVsnResult result = generateVsnStreamingly(orgId, programId, version, programName, vsnPrograms);
            watcher.checkpoint("流式XML生成完成");

            // 发布成功结果
            vsnResultPublisher.publishResultCompleted(
                    orgId,
                    programId,
                    version,
                    result.vsnFileId,
                    result.vsnPath,
                    null
            );

            log.info("VSN文件生成成功: programId={}, path={}, size={}MB, md5={}", 
                    programId, result.vsnPath, result.vsnSize / 1024 / 1024, result.vsnMd5);

        } catch (Exception e) {
            Long programId = toLong(requestPayload.get("programId"));
            Long orgId = toLong(requestPayload.get("organizationId"));
            Integer version = toInt(requestPayload.get("version"));
            log.error("生成VSN失败: programId={}, err={}", programId, e.getMessage(), e);
            vsnResultPublisher.publishResultFailed(orgId, programId, version, e.getMessage(), null);
        } finally {
            if (watcher != null) {
                watcher.finish();
            }
            MemoryMonitor.logMemoryUsage("VSN生成结束");
        }
    }

    /**
     * VSN流式生成结果
     */
    private static class StreamingVsnResult {
        final String vsnFileId;
        final String vsnPath;
        final String vsnMd5;
        final long vsnSize;

        StreamingVsnResult(String vsnFileId, String vsnPath, String vsnMd5, long vsnSize) {
            this.vsnFileId = vsnFileId;
            this.vsnPath = vsnPath;
            this.vsnMd5 = vsnMd5;
            this.vsnSize = vsnSize;
        }
    }

    /**
     * 流式生成VSN文件
     * 避免大XML内容在内存中完整存在，同时计算MD5和文件大小
     */
    private StreamingVsnResult generateVsnStreamingly(Long orgId, Long programId, Integer version, 
                                                     String programName, List<VsnProgram> vsnPrograms) throws Exception {
        
        // 预检查内存是否充足（估算需要50MB缓冲）
        if (!MemoryMonitor.hasEnoughMemory(50)) {
            log.warn("内存不足，执行垃圾回收");
            MemoryMonitor.forceGCAndLog("VSN生成前内存不足");
        }

        String vsnFileId = String.format("vsn_%d_%d", programId, System.currentTimeMillis());
        
        // 创建临时路径用于文件生成
        String basePath = fileStorageProperties.getStorage().getLocal().getBasePath();
        String tempFileName = "temp_" + vsnFileId + ".vsn";
        java.nio.file.Path tempPath = java.nio.file.Paths.get(basePath, "temp", tempFileName);
        java.nio.file.Files.createDirectories(tempPath.getParent());

        String vsnMd5;
        long vsnSize;
        
        // 使用流式写入：XML直接写入文件，同时计算MD5
        try (FileOutputStream fos = new FileOutputStream(tempPath.toFile());
             StreamingHashCalculator.HashingOutputStream hashingOut = 
                     new StreamingHashCalculator.HashingOutputStream(fos)) {
            
            // 流式写入XML内容
            vsnSize = streamingVsnXmlWriter.writeToStream(vsnPrograms, hashingOut);
            hashingOut.flush();
            
            // 获取MD5值
            vsnMd5 = hashingOut.getMD5Hex();
            
            log.debug("流式写入完成: size={}bytes, md5={}", vsnSize, vsnMd5);
        }

        // 构建最终路径并移动文件
        String finalVsnPath = buildVsnStoragePath(orgId, programId, version, programName, vsnMd5, vsnSize);
        java.nio.file.Path finalPath = java.nio.file.Paths.get(basePath, finalVsnPath);
        java.nio.file.Files.createDirectories(finalPath.getParent());
        
        // 原子性移动文件
        java.nio.file.Files.move(tempPath, finalPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        
        return new StreamingVsnResult(vsnFileId, finalVsnPath, vsnMd5, vsnSize);
    }

    // Repository负责读取

    private Long toLong(Object v) { return v == null ? null : Long.valueOf(String.valueOf(v)); }
    private Integer toInt(Object v) { return v == null ? null : Integer.valueOf(String.valueOf(v)); }

    private String buildVsnStoragePath(Long orgId, Long programId, Integer version,
                                       String programName, String md5Hex, long sizeBytes) {
        java.time.LocalDate today = java.time.LocalDate.now();
        String dateDir = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String namePart = buildNamePart(orgId, programId, programName);
        String filename = String.format("%s_%s_%d.vsn", namePart, md5Hex, sizeBytes);
        return String.format("vsn/%d/%s/%s", orgId, dateDir, filename);
    }

    private String buildNamePart(Long orgId, Long programId, String programName) {
        String safeProgramName = sanitizeFileNamePart(programName == null ? String.valueOf(programId) : programName);
        return String.format("%d-%d-%s", orgId, programId, safeProgramName);
    }


    private String sanitizeFileNamePart(String input) {
        if (input == null || input.isEmpty()) {
            return "unknown";
        }
        String s = input.trim();
        s = s.replaceAll("[\\\\/:*?\"<>|]", "-");
        s = s.replaceAll("\r|\n|\t", " ");
        s = s.replaceAll("\\s+", "-");
        if (s.length() > 80) {
            s = s.substring(0, 80);
        }
        return s;
    }
}

