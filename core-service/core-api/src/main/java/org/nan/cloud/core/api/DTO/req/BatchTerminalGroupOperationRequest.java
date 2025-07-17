package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "批量终端组操作请求DTO")
@Data
public class BatchTerminalGroupOperationRequest {

    @Schema(description = "操作类型：CREATE, UPDATE, DELETE")
    @NotNull
    private String operationType;

    @Schema(description = "终端组操作项列表")
    @NotEmpty
    private List<TerminalGroupOperationItem> items;

    @Schema(description = "终端组操作项")
    @Data
    public static class TerminalGroupOperationItem {
        @Schema(description = "终端组ID（更新和删除时必填）")
        private Long tgid;

        @Schema(description = "终端组名称")
        private String terminalGroupName;

        @Schema(description = "父级终端组ID")
        private Long parentTgid;

        @Schema(description = "描述")
        private String description;
    }
}