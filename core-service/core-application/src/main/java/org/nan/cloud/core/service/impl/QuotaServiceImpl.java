package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.OrgQuota;
import org.nan.cloud.core.repository.OrgQuotaRepository;
import org.nan.cloud.core.service.QuotaService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

    private final OrgQuotaRepository orgQuotaRepository;

    @Override
    public boolean checkQuotaAllow(Long orgId, long bytes, int files) {
        if (orgId == null || bytes < 0 || files < 0) {
            return false;
        }
        OrgQuota quota = orgQuotaRepository.findByOrgId(orgId);
        if (quota == null) {
            return false;
        }
        long usedBytes = quota.getUsedStorageSize() == null ? 0L : quota.getUsedStorageSize();
        int usedFiles = quota.getUsedFileCount() == null ? 0 : quota.getUsedFileCount();
        long maxBytes = quota.getMaxStorageSize() == null ? 0L : quota.getMaxStorageSize();
        int maxFiles = quota.getMaxFileCount() == null ? 0 : quota.getMaxFileCount();

        boolean bytesOk = maxBytes <= 0 || usedBytes + bytes <= maxBytes;
        boolean filesOk = maxFiles <= 0 || usedFiles + files <= maxFiles;
        return bytesOk && filesOk;
    }
}

