package org.nan.cloud.core.service;

import org.nan.cloud.core.api.DTO.res.OrgQuotaDetailResponse;
import org.nan.cloud.core.api.DTO.res.QuotaBreakdownResponse;
import org.nan.cloud.core.api.DTO.res.QuotaTrendResponse;
import org.nan.cloud.core.enums.QuotaOperationType;

public interface QuotaService {

    /**
     * 组织配额检查：判断本次占用是否允许
     * @param orgId 组织ID
     * @param bytes 需占用的字节数（>=0）
     * @param files 本次计数（通常为1）
     * @return 允许与否
     */
    boolean checkQuotaAllow(Long orgId, long bytes, int files);

    /**
     * 获取组织配额详情
     * @param orgId 组织ID
     * @return 配额详情响应
     */
    OrgQuotaDetailResponse getOrgQuotaDetail(Long orgId);

    /**
     * 获取配额使用趋势
     * @param orgId 组织ID
     * @param period 统计周期：DAILY, WEEKLY, MONTHLY
     * @param days 统计天数
     * @return 配额趋势响应
     */
    QuotaTrendResponse getQuotaTrend(Long orgId, String period, Integer days);

    /**
     * 获取配额使用分解
     * @param orgId 组织ID
     * @return 配额分解响应
     */
    QuotaBreakdownResponse getQuotaBreakdown(Long orgId);
}

