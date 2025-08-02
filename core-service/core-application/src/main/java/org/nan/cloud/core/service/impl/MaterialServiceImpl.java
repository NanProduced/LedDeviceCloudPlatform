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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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

        // 2. 构建公共资源组文件夹
        List<MaterialNodeTreeResponse.FolderNode> publicFolders = buildPublicFolderTree(oid);

        // 3. 构建分享文件夹
        List<MaterialNodeTreeResponse.FolderNode> sharedFolders = buildSharedFolderTree(oid, ugid);

        return MaterialNodeTreeResponse.builder()
                .rootUserGroupNode(rootUserGroupNode)
                .publicFolders(publicFolders)
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
    public void createMaterial(Material material) {
        materialRepository.createMaterial(material);
    }

    @Override
    public void updateMaterial(Material material) {
        materialRepository.updateMaterial(material);
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
     * 构建文件夹树形结构
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
                        .children(buildFolderTree(ugid, folder.getFid())) // 递归构建子文件夹
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建公共资源组文件夹树
     */
    private List<MaterialNodeTreeResponse.FolderNode> buildPublicFolderTree(Long oid) {
        // 获取公共资源组下的根级文件夹
        return buildFolderTree(null, null); // ugid为null表示公共资源组
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
}