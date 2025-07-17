package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "批量操作响应体")
@Data
public class BatchOperationResponse {

    @Schema(description = "操作总数")
    private Integer totalCount;

    @Schema(description = "成功数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failureCount;

    @Schema(description = "成功项详情")
    private List<OperationResult> successResults;

    @Schema(description = "失败项详情")
    private List<OperationResult> failureResults;

    @Schema(description = "操作结果项")
    @Data
    public static class OperationResult {
        @Schema(description = "项目ID")
        private Long itemId;

        @Schema(description = "项目名称")
        private String itemName;

        @Schema(description = "操作类型")
        private String operationType;

        @Schema(description = "结果消息")
        private String message;
    }
}