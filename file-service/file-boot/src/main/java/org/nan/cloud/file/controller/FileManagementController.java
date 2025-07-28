package org.nan.cloud.file.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.DynamicResponse;
import org.nan.cloud.file.api.FileManagementApi;
import org.nan.cloud.file.api.dto.*;
import org.nan.cloud.file.application.service.FileManagementService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文件管理控制器
 * 
 * 实现文件管理相关的REST API接口
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件管理相关接口")
public class FileManagementController implements FileManagementApi {

    private final FileManagementService fileManagementService;

    @Override
    public FileInfoResponse getFileInfo(
            @Parameter(description = "文件ID") @PathVariable String fileId) {
        
        log.info("获取文件信息 - 文件ID: {}", fileId);

        FileInfoResponse response = fileManagementService.getFileInfo(fileId);
        
        log.debug("文件信息获取成功 - 文件: {}", response.getOriginalFilename());
        
        return response;
    }

    @Override
    public FileListResponse getFileList(@RequestBody FileListRequest request) {
        
        log.info("获取文件列表 - 组织: {}, 页码: {}, 大小: {}", 
                request.getOrganizationId(), request.getPage(), request.getSize());

        FileListResponse response = fileManagementService.getFileList(request);
        
        log.debug("文件列表获取成功 - 总数: {}", response.getTotal());
        
        return response;
    }

    @Override
    public FileSearchResponse searchFiles(@RequestBody FileSearchRequest request) {
        
        log.info("搜索文件 - 关键词: {}, 组织: {}", 
                request.getKeyword(), request.getOrganizationId());

        FileSearchResponse response = fileManagementService.searchFiles(request);
        
        log.debug("文件搜索完成 - 找到: {} 个文件", response.getFiles().size());
        
        return response;
    }

    @Override
    public void downloadFile(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            HttpServletResponse response) {
        
        log.info("下载文件 - 文件ID: {}", fileId);

        try {
            fileManagementService.downloadFile(fileId, response);
            
            log.info("文件下载处理完成 - 文件ID: {}", fileId);
            
        } catch (Exception e) {
            log.error("文件下载失败 - 文件ID: {}", fileId, e);
            
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"文件下载失败: " + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("响应错误信息失败", ex);
            }
        }
    }

    @Override
    public FilePreviewUrlResponse getPreviewUrl(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            @RequestBody FilePreviewRequest request) {
        
        log.info("获取预览URL - 文件ID: {}, 预览类型: {}", fileId, request.getPreviewType());

        return fileManagementService.getPreviewUrl(fileId, request);
    }

    @Override
    public void deleteFile(
            @Parameter(description = "文件ID") @PathVariable String fileId) {
        
        log.info("删除文件 - 文件ID: {}", fileId);

        fileManagementService.deleteFile(fileId);
        
        log.info("文件删除成功 - 文件ID: {}", fileId);
    }

    @Override
    public BatchFileDeleteResponse batchDeleteFiles(
            @RequestBody BatchFileDeleteRequest request) {
        
        log.info("批量删除文件 - 文件数量: {}", request.getFileIds().size());

        BatchFileDeleteResponse response = fileManagementService.batchDeleteFiles(request);
        
        log.info("批量删除完成 - 总数: {}, 成功: {}, 失败: {}", 
                response.getTotalFiles(), response.getSuccessCount(), response.getFailedCount());
        
        return response;
    }

    @Override
    public void moveFile(@RequestBody FileMoveRequest request) {
        
        log.info("移动文件 - 文件ID: {}, 目标文件夹: {}", 
                request.getFileId(), request.getTargetFolderId());

        fileManagementService.moveFile(request);
        
        log.info("文件移动成功 - 文件ID: {}", request.getFileId());
    }

    @Override
    public FileCopyResponse copyFile(@RequestBody FileCopyRequest request) {
        
        log.info("复制文件 - 文件ID: {}, 目标文件夹: {}", 
                request.getFileId(), request.getTargetFolderId());

        FileCopyResponse response = fileManagementService.copyFile(request);
        
        log.info("文件复制成功 - 新文件ID: {}", response.getNewFileId());
        
        return response;
    }

    @Override
    public void renameFile(@RequestBody FileRenameRequest request) {
        
        log.info("重命名文件 - 文件ID: {}, 新名称: {}", 
                request.getFileId(), request.getNewFilename());

        fileManagementService.renameFile(request);
        
        log.info("文件重命名成功 - 文件ID: {}", request.getFileId());
    }

    @Override
    public FolderCreateResponse createFolder(@RequestBody FolderCreateRequest request) {
        
        log.info("创建文件夹 - 名称: {}, 父文件夹: {}", 
                request.getFolderName(), request.getParentFolderId());

        FolderCreateResponse response = fileManagementService.createFolder(request);
        
        log.info("文件夹创建成功 - 文件夹ID: {}", response.getFolderId());
        
        return response;
    }

    @Override
    public List<FolderTreeResponse> getFolderTree(
            @Parameter(description = "组织ID") @PathVariable String organizationId) {
        
        log.info("获取文件夹树 - 组织: {}", organizationId);

        List<FolderTreeResponse> response = fileManagementService.getFolderTree(organizationId);
        
        log.debug("文件夹树获取成功 - 根节点数量: {}", response.size());
        
        return response;
    }

    @Override
    public FileStatisticsResponse getFileStatistics(
            @RequestBody FileStatisticsRequest request) {
        
        log.info("获取文件统计 - 组织: {}, 类型: {}", 
                request.getOrganizationId(), request.getStatisticsType());

        FileStatisticsResponse response = fileManagementService.getFileStatistics(request);
        
        log.debug("文件统计获取成功");
        
        return response;
    }

    @Override
    public List<FileVersionResponse> getFileVersions(
            @Parameter(description = "文件ID") @PathVariable String fileId) {
        
        log.info("获取文件版本历史 - 文件ID: {}", fileId);

        List<FileVersionResponse> response = fileManagementService.getFileVersions(fileId);
        
        log.debug("文件版本历史获取成功 - 版本数量: {}", response.size());
        
        return response;
    }

    @Override
    public void restoreFileVersion(@RequestBody FileVersionRestoreRequest request) {
        
        log.info("恢复文件版本 - 文件ID: {}, 版本号: {}", 
                request.getFileId(), request.getVersionNumber());

        fileManagementService.restoreFileVersion(request);
        
        log.info("文件版本恢复成功 - 文件ID: {}, 版本: {}", 
                request.getFileId(), request.getVersionNumber());
    }
}