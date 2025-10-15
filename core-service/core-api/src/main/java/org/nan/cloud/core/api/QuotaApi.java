package org.nan.cloud.core.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nan.cloud.core.api.DTO.req.QuotaCheckRequest;
import org.nan.cloud.core.api.DTO.res.OrgQuotaDetailResponse;
import org.nan.cloud.core.api.DTO.res.QuotaBreakdownResponse;
import org.nan.cloud.core.api.DTO.res.QuotaTrendResponse;
import org.springframework.web.bind.annotation.*;

public interface QuotaApi {

    String PREFIX = "/quota";

    @PostMapping(PREFIX + "/check")
    Boolean checkQuota(@RequestBody QuotaCheckRequest request);

    @GetMapping(PREFIX + "/detail/{orgId}")
    @Operation(
            summary = "获取组织配额详情",
            description = "获取组织的配额详细信息，包括存储使用情况、告警信息和预测数据",
            tags = {"组织管理"}
    )
    OrgQuotaDetailResponse getQuotaDetail(
            @Parameter(description = "组织ID", required = true)
            @PathVariable("orgId") Long orgId
    );

    @GetMapping(PREFIX + "/trend/{orgId}")
    @Operation(
            summary = "获取配额使用趋势",
            description = "获取组织配额使用趋势数据，支持不同时间周期的统计",
            tags = {"组织管理"}
    )
    QuotaTrendResponse getQuotaTrend(
            @Parameter(description = "组织ID", required = true)
            @PathVariable("orgId") Long orgId,
            @Parameter(description = "统计周期: day, week, month", example = "day")
            @RequestParam(value = "period", defaultValue = "day") String period,
            @Parameter(description = "统计天数", example = "30")
            @RequestParam(value = "days", defaultValue = "30") Integer days
    );

    @GetMapping(PREFIX + "/breakdown/{orgId}")
    @Operation(
            summary = "获取配额使用分解",
            description = "获取组织配额使用的详细分类统计，包括按文件类型、用户组、操作类型分解",
            tags = {"组织管理"}
    )
    QuotaBreakdownResponse getQuotaBreakdown(
            @Parameter(description = "组织ID", required = true)
            @PathVariable("orgId") Long orgId
    );
}

