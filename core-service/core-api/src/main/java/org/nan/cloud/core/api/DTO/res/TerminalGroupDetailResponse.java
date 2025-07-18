package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "终端组详情响应体")
@Data
public class TerminalGroupDetailResponse {

    @Schema(description = "终端组ID")
    private Long tgid;

    @Schema(description = "终端组名称")
    private String terminalGroupName;

    @Schema(description = "组织ID")
    private Long oid;

    @Schema(description = "父级终端组ID")
    private Long parent;

    @Schema(description = "路径")
    private String path;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建者ID")
    private Long creatorId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者ID")
    private Long updaterId;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}