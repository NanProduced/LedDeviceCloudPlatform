package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nan.cloud.core.api.DTO.common.OrganizationDTO;
import org.nan.cloud.core.api.DTO.common.TerminalGroupTreeNode;

import java.util.List;

@Schema(description = "终端组树响应体")
@Data
public class TerminalGroupTreeResponse {

    @Schema(description = "组织")
    private OrganizationDTO organization;

    @Schema(description = "用户可访问的终端组树列表")
    private List<TerminalGroupTreeNode> accessibleTrees;
}