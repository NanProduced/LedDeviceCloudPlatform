package org.nan.cloud.core.service;

import org.nan.cloud.core.api.DTO.res.ListMaterialResponse;
import org.nan.cloud.core.api.DTO.res.ListSharedMaterialResponse;
import org.nan.cloud.core.api.DTO.res.MaterialNodeTreeResponse;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.event.mq.FileUploadEvent;

import java.util.List;

public interface MaterialService {

    /**
     * 构建素材管理树形结构
     * @param oid 组织ID
     * @param ugid 当前用户组ID
     * @return 树形结构
     */
    MaterialNodeTreeResponse buildMaterialStructTree(Long oid, Long ugid);

    /**
     * 查询用户组素材列表
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param fid 文件夹ID（可选）
     * @param includeSub 是否包含子用户组/子文件夹
     * @return 素材列表
     */
    List<ListMaterialResponse> listUserMaterials(Long oid, Long ugid, Long fid, boolean includeSub);

    /**
     * 查询公共素材列表
     * @param oid 组织ID
     * @param fid 文件夹ID（可选）
     * @param includeSub 是否包含子文件夹
     * @return 素材列表
     */
    List<ListMaterialResponse> listPublicMaterials(Long oid, Long fid, boolean includeSub);

    /**
     * 查询分享素材列表
     * @param oid 组织ID
     * @param ugid 当前用户组ID
     * @param fid 分享文件夹ID（可选）
     * @param includeSub 是否包含子文件夹
     * @return 分享素材列表
     */
    List<ListSharedMaterialResponse> listSharedMaterials(Long oid, Long ugid, Long fid, boolean includeSub);

    /**
     * 查询所有可见素材
     * @param oid 组织ID
     * @param ugid 当前用户组ID
     * @return 素材列表
     */
    List<ListMaterialResponse> listAllVisibleMaterials(Long oid, Long ugid);

    /**
     * 根据ID查询素材详情
     * @param mid 素材ID
     * @return 素材详情
     */
    Material getMaterialById(Long mid);

    /**
     * 创建素材
     * @param material 素材信息
     */
    void createMaterial(Material material);

    /**
     * 更新素材
     * @param material 素材信息
     */
    void updateMaterial(Material material);

    /**
     * 删除素材
     * @param mid 素材ID
     */
    void deleteMaterial(Long mid);

    /**
     * 批量删除素材
     * @param mids 素材ID列表
     */
    void deleteMaterials(List<Long> mids);

    /**
     * 基于文件上传事件创建素材
     * @param event 文件上传事件
     * @return 创建的素材ID
     */
    Long createMaterialFromFileUpload(FileUploadEvent event);

    /**
     * 基于异步上传事件创建待上传素材（占位）
     * @param event 文件上传事件
     * @return 创建的素材ID
     */
    Long createPendingMaterialFromEvent(FileUploadEvent event);

    /**
     * 根据文件ID查询素材
     * @param fileId 文件ID
     * @return 素材信息
     */
    Material getMaterialByFileId(String fileId);

    /**
     * 基于文件上传事件更新素材信息
     * @param materialId 素材ID
     * @param event 文件上传事件
     */
    void updateMaterialFromFileUpload(Long materialId, FileUploadEvent event);

    /**
     * 更新素材的元数据ID
     * @param fileId 文件ID
     * @param metadataId 元数据ID
     */
    void updateMaterialMetadata(String fileId, String metadataId);
}