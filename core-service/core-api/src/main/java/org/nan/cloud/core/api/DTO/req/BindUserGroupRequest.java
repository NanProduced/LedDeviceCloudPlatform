package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "绑定用户组到终端组请求DTO")
@Data
public class BindUserGroupRequest {

    @Schema(description = "终端组ID")
    @NotNull
    private Long tgid;

    @Schema(description = "用户组ID列表")
    @NotEmpty
    private List<Long> ugids;

    @Schema(description = "是否包含子终端组")
    private Boolean includeChildren = false;
}