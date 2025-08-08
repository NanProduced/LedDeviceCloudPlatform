package org.nan.cloud.core.service;

import org.nan.cloud.core.enums.QuotaOperationType;

public interface QuotaService {

    /**
     * 组织配额检查：判断本次占用是否允许
     * @param orgId 组织ID
     * @param bytes 需占用的字节数（>=0）
     * @param files 本次计数（通常为1）
     * @param operationType 操作类型
     * @return 允许与否
     */
    boolean checkQuotaAllow(Long orgId, long bytes, int files, QuotaOperationType operationType);
}

