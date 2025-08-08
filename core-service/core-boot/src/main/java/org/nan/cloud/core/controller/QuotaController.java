package org.nan.cloud.core.controller;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.QuotaApi;
import org.nan.cloud.core.api.quota.dto.QuotaCheckRequest;
import org.nan.cloud.core.api.quota.dto.QuotaCheckResponse;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.OrgQuotaDO;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.OrgQuotaMapper;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuotaController implements QuotaApi {

    private final OrgQuotaMapper orgQuotaMapper;

    @Override
    public QuotaCheckResponse checkQuota(QuotaCheckRequest request) {
        OrgQuotaDO quota = orgQuotaMapper.selectById(request.getOrgId());
        if (quota == null) {
            return QuotaCheckResponse.builder()
                    .allowed(false)
                    .message("组织不存在或未配置配额")
                    .build();
        }
        long usedBytes = quota.getUsedStorageSize() == null ? 0L : quota.getUsedStorageSize();
        int usedFiles = quota.getUsedFileCount() == null ? 0 : quota.getUsedFileCount();
        long maxBytes = quota.getMaxStorageSize() == null ? 0L : quota.getMaxStorageSize();
        int maxFiles = quota.getMaxFileCount() == null ? 0 : quota.getMaxFileCount();

        long requestBytes = request.getBytes() == null ? 0L : Math.max(0, request.getBytes());
        int requestFiles = request.getFiles() == null ? 0 : Math.max(0, request.getFiles());

        boolean bytesOk = maxBytes <= 0 || usedBytes + requestBytes <= maxBytes;
        boolean filesOk = maxFiles <= 0 || usedFiles + requestFiles <= maxFiles;

        long remainingBytes = maxBytes <= 0 ? Long.MAX_VALUE : Math.max(0, maxBytes - usedBytes);
        int remainingFiles = maxFiles <= 0 ? Integer.MAX_VALUE : Math.max(0, maxFiles - usedFiles);

        return QuotaCheckResponse.builder()
                .allowed(bytesOk && filesOk)
                .remainingBytes(remainingBytes)
                .remainingFiles(remainingFiles)
                .usedBytes(usedBytes)
                .usedFiles(usedFiles)
                .maxBytes(maxBytes)
                .maxFiles(maxFiles)
                .message((bytesOk && filesOk) ? null : "组织配额不足")
                .build();
    }
}

