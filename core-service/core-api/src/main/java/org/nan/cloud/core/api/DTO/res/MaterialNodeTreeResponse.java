package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "素材管理 - 素材文件夹结构树响应")
public class MaterialNodeTreeResponse {

    /**
     * 用户组根目录
     */
    @Schema(description = "用户组根目录")
    private GroupNode rootUserGroupNode;

    @Schema(description = "公共资源组根节点")
    private FolderNode publicRootFolder;

    @Schema(description = "分享文件夹")
    private List<FolderNode> sharedFolders;

    /**
     * 文件夹树节点（用户组根目录节点）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "用户组根目录节点")
    public static class GroupNode {

        @Schema(description = "用户组Id")
        private Long ugid;

        @Schema(description = "用户组名称")
        private String groupName;

        @Schema(description = "父级组")
        private Long parent;

        @Schema(description = "路径")
        private String path;

        @Schema(description = "用户组根目录下文件夹")
        private List<FolderNode> folders;

        @Schema(description = "子用户组")
        private List<GroupNode> children;
    }


    /**
     * 文件夹树节点（文件夹节点）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "文件夹节点")
    public static class FolderNode {

        @Schema(description = "文件夹Id")
        private Long fid;

        @Schema(description = "文件夹名称")
        private String folderName;

        @Schema(description = "父级文件夹")
        private Long parent;

        @Schema(description = "文件夹路径")
        private String path;

        @Schema(description = "子文件夹")
        private List<FolderNode> children;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "分享文件夹节点")
    public static class SharedFolderNode {

        @Schema(description = "分享文件夹Id")
        private Long fid;

        @Schema(description = "文件夹名称")
        private String folderName;

        @Schema(description = "父级文件夹")
        private Long parent;

        @Schema(description = "源文件夹所属用户组")
        private Long sharedFrom;

        @Schema(description = "分享到的用户组")
        private Long sharedTo;

        @Schema(description = "子文件夹")
        private List<FolderNode> children;

    }
}
