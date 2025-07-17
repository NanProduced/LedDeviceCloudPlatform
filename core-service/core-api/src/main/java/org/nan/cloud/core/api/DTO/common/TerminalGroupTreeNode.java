package org.nan.cloud.core.api.DTO.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "终端组树节点")
@Data
@Builder
public class TerminalGroupTreeNode {

    private Long tgid;

    private String tgName;

    private Long parent;

    private String path;

    private Map<Long, String> pathMap;

    private List<TerminalGroupTreeNode> children;

    private Integer childrenCount;

    private String description;
}