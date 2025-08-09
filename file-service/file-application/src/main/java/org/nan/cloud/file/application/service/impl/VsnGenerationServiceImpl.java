package org.nan.cloud.file.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.service.VsnGenerationService;
import org.nan.cloud.file.application.port.VsnResultPublisher;
import org.nan.cloud.file.application.repository.ProgramContentRepository;
import org.nan.cloud.file.application.service.validation.VsnSchemaValidator;
import org.nan.cloud.file.application.port.VsnXmlWriter;
import org.nan.cloud.program.document.ProgramContent;
import org.nan.cloud.program.document.VsnProgram;
import org.nan.cloud.file.application.config.FileStorageProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

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

    @Override
    public void generate(Map<String, Object> requestPayload) {
        try {
            Long orgId = toLong(requestPayload.get("organizationId"));
            Long programId = toLong(requestPayload.get("programId"));
            Integer version = toInt(requestPayload.get("version"));
            String contentId = String.valueOf(requestPayload.get("contentId"));
            String programName = requestPayload.get("programName") == null ? null : String.valueOf(requestPayload.get("programName"));

            // 1) 从 Mongo 读取 ProgramContent（按 contentId）
            ProgramContent content = programContentRepository.findById(contentId);

            // 2) 校验 ProgramContent → VsnProgram 列表存在性与基本结构
            List<VsnProgram> vsnPrograms = content.getVsnPrograms();
            if (vsnPrograms == null || vsnPrograms.isEmpty()) {
                throw new IllegalArgumentException("vsnPrograms 为空，无法生成VSN");
            }
            vsnSchemaValidator.validate(vsnPrograms);

            // 3) 写出XML并保存到本地存储（按组织隔离）
            String vsnXml = vsnXmlWriter.write(vsnPrograms);
            byte[] vsnBytes = vsnXml == null ? new byte[0] : vsnXml.getBytes(StandardCharsets.UTF_8);
            String vsnMd5 = calculateMD5Hex(vsnBytes);
            long vsnSize = vsnBytes.length;

            String vsnFileId = String.format("vsn_%d_%d", programId, System.currentTimeMillis());
            String vsnPath = buildVsnStoragePath(orgId, programId, version, programName, vsnMd5, vsnSize);
            writeBytesToLocalFile(vsnPath, vsnBytes);

            // 发布成功结果
            vsnResultPublisher.publishResultCompleted(
                    orgId,
                    programId,
                    version,
                    vsnFileId,
                    vsnPath,
                    null
            );

        } catch (Exception e) {
            Long programId = toLong(requestPayload.get("programId"));
            Long orgId = toLong(requestPayload.get("organizationId"));
            Integer version = toInt(requestPayload.get("version"));
            log.error("生成VSN失败: programId={}, err={}", programId, e.getMessage(), e);
            vsnResultPublisher.publishResultFailed(orgId, programId, version, e.getMessage(), null);
        }
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

    private void writeBytesToLocalFile(String relativePath, byte[] content) {
        String basePath = fileStorageProperties.getStorage().getLocal().getBasePath();
        java.nio.file.Path target = java.nio.file.Paths.get(basePath, relativePath);
        try {
            java.nio.file.Files.createDirectories(target.getParent());
            java.nio.file.Files.write(target, content == null ? new byte[0] : content,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE);
        } catch (Exception e) {
            throw new RuntimeException("写入VSN文件失败:" + e.getMessage(), e);
        }
    }

    private String calculateMD5Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data == null ? new byte[0] : data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算VSN内容MD5失败:" + e.getMessage(), e);
        }
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

