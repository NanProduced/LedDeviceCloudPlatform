package org.nan.cloud.file.application.repository;

/**
 * 组织存储空间配额存储
 * @author Nan
 */
public interface OrgQuotaRepository {

    /**
     * 验证空间存储
     * @param oid
     * @param usedBytes
     * @return
     */
    boolean hasEnoughSpace(Long oid, Long usedBytes, Long usedFileCount);

}
