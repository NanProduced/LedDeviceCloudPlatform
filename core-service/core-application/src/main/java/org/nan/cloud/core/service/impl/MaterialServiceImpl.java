package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.api.DTO.res.ListMaterialResponse;
import org.nan.cloud.core.api.DTO.res.ListSharedMaterialResponse;
import org.nan.cloud.core.api.DTO.res.MaterialNodeTreeResponse;
import org.nan.cloud.core.domain.Folder;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.domain.MaterialShareRel;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.repository.FolderRepository;
import org.nan.cloud.core.repository.MaterialRepository;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.nan.cloud.core.service.MaterialService;
import org.nan.cloud.core.event.mq.FileUploadEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final FolderRepository folderRepository;
    private final UserGroupRepository userGroupRepository;

    @Override
    public MaterialNodeTreeResponse buildMaterialStructTree(Long oid, Long ugid) {
        log.info("Building material structure tree for org: {}, userGroup: {}", oid, ugid);

        // 1. 构建用户组根目录节点
        MaterialNodeTreeResponse.GroupNode rootUserGroupNode = buildUserGroupTree(oid, ugid);

        // 2. 构建公共资源组根文件夹
        MaterialNodeTreeResponse.FolderNode publicRootFolder = buildPublicRootFolder(oid);

        // 3. 构建分享文件夹
        List<MaterialNodeTreeResponse.FolderNode> sharedFolders = buildSharedFolderTree(oid, ugid);

        return MaterialNodeTreeResponse.builder()
                .rootUserGroupNode(rootUserGroupNode)
                .publicRootFolder(publicRootFolder)
                .sharedFolders(sharedFolders)
                .build();
    }

    @Override
    public List<ListMaterialResponse> listUserMaterials(Long oid, Long ugid, Long fid, boolean includeSub) {
        log.info("Listing user materials for org: {}, userGroup: {}, folder: {}, includeSub: {}", 
                oid, ugid, fid, includeSub);

        List<Material> materials = materialRepository.listMaterialsByUserGroup(oid, ugid, fid, includeSub);
        return convertToMaterialResponses(materials);
    }

    @Override
    public List<ListMaterialResponse> listPublicMaterials(Long oid, Long fid, boolean includeSub) {
        log.info("Listing public materials for org: {}, folder: {}, includeSub: {}", oid, fid, includeSub);

        List<Material> materials = materialRepository.listPublicMaterials(oid, fid, includeSub);
        return convertToMaterialResponses(materials);
    }

    @Override
    public List<ListSharedMaterialResponse> listSharedMaterials(Long oid, Long ugid, Long fid, boolean includeSub) {
        log.info("Listing shared materials for org: {}, userGroup: {}, folder: {}, includeSub: {}", 
                oid, ugid, fid, includeSub);

        List<MaterialShareRel> sharedMaterials = materialRepository.listSharedMaterials(oid, ugid, fid, includeSub);
        return convertToSharedMaterialResponses(sharedMaterials);
    }

    @Override
    public List<ListMaterialResponse> listAllVisibleMaterials(Long oid, Long ugid) {
        log.info("Listing all visible materials for org: {}, userGroup: {}", oid, ugid);

        List<Material> materials = materialRepository.listAllVisibleMaterials(oid, ugid);
        return convertToMaterialResponses(materials);
    }

    @Override
    public Material getMaterialById(Long mid) {
        return materialRepository.getMaterialById(mid);
    }

    @Override
    public void deleteMaterial(Long mid) {
        materialRepository.deleteMaterial(mid);
    }

    @Override
    public void deleteMaterials(List<Long> mids) {
        materialRepository.deleteMaterials(mids);
    }

    // ========================= 私有方法 =========================

    /**
     * 构建用户组树形结构
     */
    private MaterialNodeTreeResponse.GroupNode buildUserGroupTree(Long oid, Long ugid) {
        // 获取当前用户组信息
        UserGroup currentUserGroup = userGroupRepository.getUserGroupById(ugid);
        if (currentUserGroup == null) {
            return null;
        }

        // 构建用户组节点
        MaterialNodeTreeResponse.GroupNode groupNode = MaterialNodeTreeResponse.GroupNode.builder()
                .ugid(currentUserGroup.getUgid())
                .groupName(currentUserGroup.getName())
                .parent(currentUserGroup.getParent())
                .path(currentUserGroup.getPath())
                .folders(buildFolderTree(ugid, null)) // 构建该用户组下的文件夹树
                .children(buildChildUserGroups(oid, ugid)) // 构建子用户组
                .build();

        return groupNode;
    }

    /**
     * 构建子用户组
     */
    private List<MaterialNodeTreeResponse.GroupNode> buildChildUserGroups(Long oid, Long parentUgid) {
        List<UserGroup> childUserGroups = userGroupRepository.getDirectUserGroupsByParent(parentUgid);
        return childUserGroups.stream()
                .filter(userGroup -> userGroup.getOid().equals(oid)) // 确保是同一组织
                .map(userGroup -> MaterialNodeTreeResponse.GroupNode.builder()
                        .ugid(userGroup.getUgid())
                        .groupName(userGroup.getName())
                        .parent(userGroup.getParent())
                        .path(userGroup.getPath())
                        .folders(buildFolderTree(userGroup.getUgid(), null))
                        .children(buildChildUserGroups(oid, userGroup.getUgid())) // 递归构建
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建文件夹树形结构（用户组文件夹）
     */
    private List<MaterialNodeTreeResponse.FolderNode> buildFolderTree(Long ugid, Long parentFid) {
        List<Folder> folders;
        
        if (parentFid == null) {
            // 获取根级文件夹
            folders = folderRepository.getRootFoldersByUserGroup(ugid);
        } else {
            // 获取子文件夹
            folders = folderRepository.getChildFolders(parentFid);
        }
        
        return folders.stream()
                .map(folder -> MaterialNodeTreeResponse.FolderNode.builder()
                        .fid(folder.getFid())
                        .folderName(folder.getFolderName())
                        .parent(folder.getParent())
                        .path(folder.getPath())
                        // 优化：如果有path，使用path优化查询，否则使用原有递归方法
                        .children(folder.getPath() != null && !folder.getPath().isEmpty()
                                ? buildFolderTreeRecursivelyByPath(folder.getPath())
                                : buildFolderTree(ugid, folder.getFid()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建公共资源组根文件夹
     */
    private MaterialNodeTreeResponse.FolderNode buildPublicRootFolder(Long oid) {
        // 获取指定组织的公共资源组根文件夹
        Folder publicRootFolder = folderRepository.getPublicRootFolderByOrg(oid);
        
        if (publicRootFolder == null) {
            return null;
        }

        return MaterialNodeTreeResponse.FolderNode.builder()
                .fid(publicRootFolder.getFid())
                .folderName(publicRootFolder.getFolderName())
                .parent(publicRootFolder.getParent())
                .path(publicRootFolder.getPath())
                .children(buildFolderTreeRecursivelyByPath(publicRootFolder.getPath())) // 使用path优化查询
                .build();
    }

    /**
     * 基于path字段递归构建文件夹树（优化版本）
     */
    private List<MaterialNodeTreeResponse.FolderNode> buildFolderTreeRecursivelyByPath(String parentPath) {
        // 使用path字段优化查询，避免递归数据库查询
        List<Folder> childFolders = folderRepository.getDirectChildFoldersByParentPath(parentPath);
        
        return childFolders.stream()
                .map(folder -> MaterialNodeTreeResponse.FolderNode.builder()
                        .fid(folder.getFid())
                        .folderName(folder.getFolderName())
                        .parent(folder.getParent())
                        .path(folder.getPath())
                        .children(buildFolderTreeRecursivelyByPath(folder.getPath())) // 递归构建子文件夹
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建分享文件夹树
     */
    private List<MaterialNodeTreeResponse.FolderNode> buildSharedFolderTree(Long oid, Long ugid) {
        // 获取分享给当前用户组的文件夹
        return new ArrayList<>(); // 待实现
    }

    /**
     * 转换Material列表为ListMaterialResponse列表
     */
    private List<ListMaterialResponse> convertToMaterialResponses(List<Material> materials) {
        return materials.stream()
                .map(this::convertToMaterialResponse)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个Material为ListMaterialResponse
     */
    private ListMaterialResponse convertToMaterialResponse(Material material) {
        return ListMaterialResponse.builder()
                .mid(material.getMid())
                .materialName(material.getMaterialName())
                .fileId(material.getFileId())
                .materialType(material.getMaterialType())
                .fileSize(material.getOriginalFileSize())
                .fileSizeFormatted(material.getFormattedFileSize())
                .mimeType(material.getMimeType())
                .fileExtension(material.getFileExtension())
                .fileStatus(material.getFileStatus())
                .fileStatusDesc(material.getFileStatusDescription())
                .processProgress(calculateProcessProgress(material.getFileStatus()))
                .description(material.getDescription())
                .usageCount(material.getUsageCount())
                .ugid(material.getUgid())
                .fid(material.getFid())
                .uploadedBy(material.getUploadedBy())
                .uploaderName(getUploaderName(material.getUploadedBy())) // 需要实现用户名查询
                .uploadTime(material.getUploadTime())
                .createTime(material.getCreateTime())
                .updateTime(material.getUpdateTime())
                .build();
    }

    /**
     * 转换MaterialShareRel列表为ListSharedMaterialResponse列表
     */
    private List<ListSharedMaterialResponse> convertToSharedMaterialResponses(List<MaterialShareRel> sharedMaterials) {
        return sharedMaterials.stream()
                .map(this::convertToSharedMaterialResponse)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个MaterialShareRel为ListSharedMaterialResponse
     */
    private ListSharedMaterialResponse convertToSharedMaterialResponse(MaterialShareRel shareRel) {
        // 基础Material信息需要通过resourceId查询
        Material material = materialRepository.getMaterialById(shareRel.getResourceId());
        
        return ListSharedMaterialResponse.builder()
                // 继承父类字段
                .mid(material.getMid())
                .materialName(material.getMaterialName())
                .fileId(material.getFileId())
                .materialType(material.getMaterialType())
                .fileSize(material.getOriginalFileSize())
                .fileSizeFormatted(material.getFormattedFileSize())
                .mimeType(material.getMimeType())
                .fileExtension(material.getFileExtension())
                .fileStatus(material.getFileStatus())
                .fileStatusDesc(material.getFileStatusDescription())
                .processProgress(calculateProcessProgress(material.getFileStatus()))
                .description(material.getDescription())
                .usageCount(material.getUsageCount())
                .ugid(material.getUgid())
                .fid(material.getFid())
                .uploadedBy(material.getUploadedBy())
                .uploaderName(getUploaderName(material.getUploadedBy()))
                .uploadTime(material.getUploadTime())
                .createTime(material.getCreateTime())
                .updateTime(material.getUpdateTime())
                // 分享相关字段
                .shareId(shareRel.getShareId())
                .sharedFrom(shareRel.getSharedFrom())
                .sharedFromGroupName(getUserGroupName(shareRel.getSharedFrom()))
                .sharedTo(shareRel.getSharedTo())
                .sharedToGroupName(getUserGroupName(shareRel.getSharedTo()))
                .sharedBy(shareRel.getSharedBy())
                .sharedByUserName(getUploaderName(shareRel.getSharedBy()))
                .sharedTime(shareRel.getSharedTime())
                .resourceType(shareRel.getResourceType())
                .build();
    }

    /**
     * 计算处理进度
     */
    private Integer calculateProcessProgress(Integer fileStatus) {
        if (fileStatus == null) {
            return null;
        }
        return switch (fileStatus) {
            case 1 -> 100; // 已完成
            case 2 -> 65;  // 处理中，这里返回一个示例值，实际应该从其他地方获取
            case 3 -> 0;   // 失败
            default -> null;
        };
    }

    /**
     * 获取上传者姓名
     * TODO: 需要集成用户服务
     */
    private String getUploaderName(Long uploadedBy) {
        if (uploadedBy == null) {
            return null;
        }
        // 这里应该调用用户服务获取用户名
        return "用户" + uploadedBy; // 临时实现
    }

    /**
     * 获取用户组名称
     */
    private String getUserGroupName(Long ugid) {
        if (ugid == null) {
            return null;
        }
        UserGroup userGroup = userGroupRepository.getUserGroupById(ugid);
        return userGroup != null ? userGroup.getName() : null;
    }

    @Override
    @Transactional
    public Long createPendingMaterialFromEvent(FileUploadEvent event) {
        log.info("创建待上传素材占位 - 文件ID: {}, 任务ID: {}", event.getFileId(), event.getTaskId());
        
        try {
            // 创建Material业务实体（状态为PENDING，metadataId暂时为null）
            // 等待file-service的FILE_PROCESSING_COMPLETED事件来更新metadataId
            Material material = buildPendingMaterialFromEvent(event, null);
            materialRepository.createMaterial(material);
            
            log.info("待上传素材占位创建成功 - 素材ID: {}, 文件ID: {}", material.getMid(), event.getFileId());
            return material.getMid();
            
        } catch (Exception e) {
            log.error("创建待上传素材占位失败 - 文件ID: {}, 错误: {}", event.getFileId(), e.getMessage(), e);
            throw new RuntimeException("创建待上传素材占位失败", e);
        }
    }

    @Override
    @Transactional
    public void updateMaterialFromFileUpload(Long materialId, FileUploadEvent event) {
        try {
            Material existingMaterial = materialRepository.getMaterialById(materialId);
            if (existingMaterial == null) {
                throw new IllegalArgumentException("素材不存在 - ID: " + materialId);
            }
            
            // 只更新Material表中的业务相关字段
            if (event.getFileType() != null) {
                existingMaterial.setMaterialType(determineMaterialType(event.getFileType()));
            }
            
            // 设置上传完成时间和更新时间
            existingMaterial.setUploadTime(LocalDateTime.now());
            
            materialRepository.updateMaterial(existingMaterial);
            
            log.info("素材业务信息更新成功 - 素材ID: {}, 文件ID: {}, 类型: {}", 
                    materialId, event.getFileId(), existingMaterial.getMaterialType());
            
        } catch (Exception e) {
            log.error("更新素材业务信息失败 - 素材ID: {}, 错误: {}", materialId, e.getMessage(), e);
            throw new RuntimeException("更新素材业务信息失败", e);
        }
    }

    @Override
    @Transactional
    public void updateMaterialMetadataById(Long materialId, String metadataId) {
        log.debug("🔒 使用安全的素材ID更新元数据 - materialId: {}, metadataId: {}", materialId, metadataId);
        
        try {
            Material material = materialRepository.getMaterialById(materialId);
            if (material == null) {
                log.warn("⚠️ 素材不存在 - materialId: {}", materialId);
                throw new IllegalArgumentException("素材不存在 - ID: " + materialId);
            }
            
            material.setMetaDataId(metadataId);
            material.setUpdateTime(LocalDateTime.now());
            materialRepository.updateMaterial(material);
            
            log.info("✅ 素材元数据更新成功 - materialId: {}, metadataId: {}", materialId, metadataId);
            
        } catch (Exception e) {
            log.error("❌ 更新素材元数据失败 - materialId: {}, 错误: {}", materialId, e.getMessage(), e);
            throw new RuntimeException("更新素材元数据失败", e);
        }
    }



    /**
     * 构建待上传Material业务实体
     */
    private Material buildPendingMaterialFromEvent(FileUploadEvent event, String metadataId) {
        
        // 确保必填字段不为空
        String fileId = event.getFileId();
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("文件ID不能为空");
        }
        
        // 确保uploadedBy字段有默认值（避免数据库约束违反）
        Long uploadedBy = Long.valueOf(event.getUserId());
        
        return Material.builder()
                // === 核心业务字段 ===
                .materialName(event.getMaterialName())
                .fileId(fileId)
                .oid(Long.valueOf(event.getOrganizationId()))
                .ugid(Long.valueOf(event.getUserGroupId()))
                .fid(Objects.nonNull(event.getFolderId()) ? Long.valueOf(event.getFolderId()) : null)
                .materialType(determineMaterialType(event.getFileType()))
                .description(event.getDescription())
                .usageCount(0L)
                .uploadedBy(uploadedBy) // 确保非空
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                // === MongoDB元数据关联 ===
                .metaDataId(metadataId)
                .build();
    }

    /**
     * 确定素材类型
     */
    private String determineMaterialType(String fileType) {
        if (fileType == null) {
            return "UNKNOWN";
        }
        
        return switch (fileType.toUpperCase()) {
            case "IMAGE" -> "IMAGE";
            case "VIDEO" -> "VIDEO";
            case "AUDIO" -> "AUDIO";
            default -> "DOCUMENT";
        };
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }
}