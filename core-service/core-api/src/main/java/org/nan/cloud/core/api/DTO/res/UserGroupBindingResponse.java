package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户组绑定响应体")
@Data
public class UserGroupBindingResponse {

    @Schema(description = "绑定ID")
    private Long id;

    @Schema(description = "终端组ID")
    private Long tgid;

    @Schema(description = "终端组名称")
    private String terminalGroupName;

    @Schema(description = "用户组ID")
    private Long ugid;

    @Schema(description = "用户组名称")
    private String userGroupName;

    @Schema(description = "是否包含子终端组")
    private Boolean includeChildren;

    @Schema(description = "创建者ID")
    private Long creatorId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}