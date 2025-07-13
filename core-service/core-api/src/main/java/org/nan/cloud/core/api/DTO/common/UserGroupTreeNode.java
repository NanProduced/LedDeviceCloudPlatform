package org.nan.cloud.core.api.DTO.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "用户组树节点")
@Data
@Builder
public class UserGroupTreeNode {

    private Long ugid;

    private String ugName;

    private Long parent;

    private String path;

    private Map<Long, String> pathMap;

    private List<UserGroupTreeNode> children;
}
