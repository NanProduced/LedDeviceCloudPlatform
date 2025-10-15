package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "绑定冲突检查响应体")
@Data
public class BindingConflictCheckResponse {

    @Schema(description = "是否存在冲突")
    private Boolean hasConflict;

    @Schema(description = "冲突类型")
    private ConflictType conflictType;

    @Schema(description = "冲突的终端组ID列表")
    private List<Long> conflictTerminalGroupIds;

    @Schema(description = "冲突描述信息")
    private String conflictMessage;

    @Schema(description = "建议的解决方案")
    private ConflictResolution suggestedResolution;

    @Schema(description = "冲突的终端组详情")
    private List<ConflictTerminalGroupDetail> conflictTerminalGroups;

    @Schema(description = "冲突类型枚举")
    public enum ConflictType {
        @Schema(description = "父组已绑定（包含子组），现在要绑定子组")
        PARENT_ALREADY_BOUND_WITH_CHILDREN,
        
        @Schema(description = "子组已绑定，现在要绑定父组（包含子组）")
        CHILDREN_ALREADY_BOUND,
        
        @Schema(description = "直接重复绑定")
        DIRECT_DUPLICATE
    }

    @Schema(description = "冲突解决方案枚举")
    public enum ConflictResolution {
        @Schema(description = "拒绝绑定")
        REJECT_BINDING,
        
        @Schema(description = "替换现有绑定")
        REPLACE_EXISTING,
        
        @Schema(description = "移除冲突的子组绑定")
        REMOVE_CONFLICTING_CHILDREN,
        
        @Schema(description = "用户确认后继续")
        CONFIRM_AND_PROCEED
    }

    @Schema(description = "冲突终端组详情")
    @Data
    public static class ConflictTerminalGroupDetail {
        @Schema(description = "终端组ID")
        private Long tgid;
        
        @Schema(description = "终端组名称")
        private String terminalGroupName;
        
        @Schema(description = "是否包含子组")
        private Boolean includeChildren;
        
        @Schema(description = "绑定关系说明")
        private String bindingDescription;
    }
}