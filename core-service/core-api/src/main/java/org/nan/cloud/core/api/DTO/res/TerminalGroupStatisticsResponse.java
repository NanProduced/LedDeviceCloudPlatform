package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "终端组统计信息响应体")
@Data
public class TerminalGroupStatisticsResponse {

    @Schema(description = "终端组ID")
    private Long tgid;

    @Schema(description = "终端组名称")
    private String terminalGroupName;

    @Schema(description = "直属设备数量")
    private Integer directDeviceCount;

    @Schema(description = "包含子组设备总数")
    private Integer totalDeviceCount;

    @Schema(description = "直属子组数量")
    private Integer directChildCount;

    @Schema(description = "包含子组总数")
    private Integer totalChildCount;

    @Schema(description = "在线设备数量")
    private Integer onlineDeviceCount;

    @Schema(description = "离线设备数量")
    private Integer offlineDeviceCount;
}