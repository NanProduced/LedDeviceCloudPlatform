package org.nan.cloud.file.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nan.cloud.common.web.DynamicResponse;
import org.nan.cloud.file.api.dto.*;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文件管理API接口
 * 
 * 功能说明：
 * - 文件信息查询和管理
 * - 文件下载和预览
 * - 文件夹结构管理
 * - 文件权限控制
 * - 文件版本管理
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Tag(name = "文件管理", description = "文件管理相关接口")
@RequestMapping("/file/management")
public interface FileManagementApi {

    /**
     * 获取文件信息
     * 
     * @param fileId 文件ID
     * @return 文件详细信息
     */
    @Operation(summary = "获取文件信息", description = "根据文件ID获取文件的详细信息")
    @GetMapping("/{fileId}")
    DynamicResponse<FileInfoResponse> getFileInfo(
            @Parameter(description = "文件ID") @PathVariable String fileId);

    /**
     * 获取文件列表
     * 
     * @param request 文件查询请求
     * @return 文件列表
     */
    @Operation(summary = "获取文件列表", description = "分页查询文件列表")
    @PostMapping("/list")
    DynamicResponse<FileListResponse> getFileList(
            @RequestBody FileListRequest request);

    /**
     * 搜索文件
     * 
     * @param request 文件搜索请求
     * @return 搜索结果
     */
    @Operation(summary = "搜索文件", description = "根据关键词搜索文件")
    @PostMapping("/search")
    DynamicResponse<FileSearchResponse> searchFiles(
            @RequestBody FileSearchRequest request);

    /**
     * 下载文件
     * 
     * @param fileId 文件ID
     * @param response HTTP响应对象
     */
    @Operation(summary = "下载文件", description = "根据文件ID下载文件")
    @GetMapping("/download/{fileId}")
    void downloadFile(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            HttpServletResponse response);

    /**
     * 获取文件预览URL
     * 
     * @param fileId 文件ID
     * @param request 预览请求参数
     * @return 预览URL
     */
    @Operation(summary = "获取预览URL", description = "获取文件的预览访问URL")
    @PostMapping("/preview-url/{fileId}")
    DynamicResponse<FilePreviewUrlResponse> getPreviewUrl(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            @RequestBody FilePreviewRequest request);

    /**
     * 删除文件
     * 
     * @param fileId 文件ID
     * @return 删除结果
     */
    @Operation(summary = "删除文件", description = "删除指定的文件")
    @DeleteMapping("/{fileId}")
    DynamicResponse<Void> deleteFile(
            @Parameter(description = "文件ID") @PathVariable String fileId);

    /**
     * 批量删除文件
     * 
     * @param request 批量删除请求
     * @return 删除结果
     */
    @Operation(summary = "批量删除文件", description = "批量删除多个文件")
    @PostMapping("/batch-delete")
    DynamicResponse<BatchFileDeleteResponse> batchDeleteFiles(
            @RequestBody BatchFileDeleteRequest request);

    /**
     * 移动文件
     * 
     * @param request 文件移动请求
     * @return 移动结果
     */
    @Operation(summary = "移动文件", description = "将文件移动到指定目录")
    @PostMapping("/move")
    DynamicResponse<Void> moveFile(
            @RequestBody FileMoveRequest request);

    /**
     * 复制文件
     * 
     * @param request 文件复制请求
     * @return 复制结果
     */
    @Operation(summary = "复制文件", description = "复制文件到指定目录")
    @PostMapping("/copy")
    DynamicResponse<FileCopyResponse> copyFile(
            @RequestBody FileCopyRequest request);

    /**
     * 重命名文件
     * 
     * @param request 文件重命名请求
     * @return 重命名结果
     */
    @Operation(summary = "重命名文件", description = "修改文件名称")
    @PostMapping("/rename")
    DynamicResponse<Void> renameFile(
            @RequestBody FileRenameRequest request);

    /**
     * 创建文件夹
     * 
     * @param request 创建文件夹请求
     * @return 创建结果
     */
    @Operation(summary = "创建文件夹", description = "创建新的文件夹")
    @PostMapping("/folder/create")
    DynamicResponse<FolderCreateResponse> createFolder(
            @RequestBody FolderCreateRequest request);

    /**
     * 获取文件夹树形结构
     * 
     * @param organizationId 组织ID
     * @return 文件夹树形结构
     */
    @Operation(summary = "获取文件夹树", description = "获取组织的文件夹树形结构")
    @GetMapping("/folder/tree/{organizationId}")
    DynamicResponse<List<FolderTreeResponse>> getFolderTree(
            @Parameter(description = "组织ID") @PathVariable String organizationId);

    /**
     * 获取文件统计信息
     * 
     * @param request 统计请求参数
     * @return 统计信息
     */
    @Operation(summary = "文件统计信息", description = "获取文件的统计信息")
    @PostMapping("/statistics")
    DynamicResponse<FileStatisticsResponse> getFileStatistics(
            @RequestBody FileStatisticsRequest request);

    /**
     * 获取文件版本历史
     * 
     * @param fileId 文件ID
     * @return 版本历史列表
     */
    @Operation(summary = "文件版本历史", description = "获取文件的版本历史记录")
    @GetMapping("/versions/{fileId}")
    DynamicResponse<List<FileVersionResponse>> getFileVersions(
            @Parameter(description = "文件ID") @PathVariable String fileId);

    /**
     * 恢复文件版本
     * 
     * @param request 版本恢复请求
     * @return 恢复结果
     */
    @Operation(summary = "恢复文件版本", description = "将文件恢复到指定版本")
    @PostMapping("/versions/restore")
    DynamicResponse<Void> restoreFileVersion(
            @RequestBody FileVersionRestoreRequest request);
}