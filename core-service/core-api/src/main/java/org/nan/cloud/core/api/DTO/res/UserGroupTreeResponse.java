package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nan.cloud.core.api.DTO.common.OrganizationDTO;
import org.nan.cloud.core.api.DTO.common.UserGroupTreeNode;

@Schema(description = "用户组树响应体")
@Data
public class UserGroupTreeResponse {

    @Schema(description = "组织")
    private OrganizationDTO organization;

    @Schema(description = "根终端组")
    private UserGroupTreeNode root;

}
