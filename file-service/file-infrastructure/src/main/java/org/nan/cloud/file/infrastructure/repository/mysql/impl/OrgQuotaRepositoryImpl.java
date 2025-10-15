package org.nan.cloud.file.infrastructure.repository.mysql.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.repository.OrgQuotaRepository;
import org.nan.cloud.file.infrastructure.repository.mysql.DO.OrgQuotaDO;
import org.nan.cloud.file.infrastructure.repository.mysql.mapper.OrgQuotaMapper;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrgQuotaRepositoryImpl implements OrgQuotaRepository {

    private final OrgQuotaMapper orgQuotaMapper;

    @Override
    public boolean hasEnoughSpace(Long oid, Long usedBytes, Long usedFileCount) {
        if (oid == null || usedBytes == null || usedBytes < 0) {
            return false;
        }
        OrgQuotaDO quota = orgQuotaMapper.selectById(oid);
        if (quota == null) {
            log.warn("Org quota not found for oid={}", oid);
            return false;
        }
        long usedBytesCurrent = quota.getUsedStorageSize() == null ? 0L : quota.getUsedStorageSize();
        int usedFilesCurrent = quota.getUsedFileCount() == null ? 0 : quota.getUsedFileCount();
        long maxBytes = quota.getMaxStorageSize() == null ? 0L : quota.getMaxStorageSize();
        int maxFiles = quota.getMaxFileCount() == null ? 0 : quota.getMaxFileCount();

        long requestedBytes = usedBytes;
        int requestedFiles = usedFileCount == null ? 0 : usedFileCount.intValue();

        boolean bytesOk = maxBytes <= 0 || usedBytesCurrent + requestedBytes <= maxBytes;
        boolean filesOk = maxFiles <= 0 || usedFilesCurrent + requestedFiles <= maxFiles;
        return bytesOk && filesOk;
    }
}

