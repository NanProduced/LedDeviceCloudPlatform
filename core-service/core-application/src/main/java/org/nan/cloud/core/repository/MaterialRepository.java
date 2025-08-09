package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.domain.MaterialShareRel;

import java.util.List;

public interface MaterialRepository {

    /**
     * 根据素材ID查询素材
     */
    Material getMaterialById(Long mid);

    /**
     * 查询用户组下的所有素材
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param fid 文件夹ID（可选）
     * @param includeSub 是否包含子用户组或子文件夹
     * @return 素材列表
     */
    List<Material> listMaterialsByUserGroup(Long oid, Long ugid, Long fid, boolean includeSub);

    /**
     * 查询公共资源组下的素材
     * @param oid 组织ID
     * @param fid 文件夹ID（可选）
     * @param includeSub 是否包含子文件夹
     * @return 素材列表
     */
    List<Material> listPublicMaterials(Long oid, Long fid, boolean includeSub);

    /**
     * 查询分享给指定用户组的素材
     * @param oid 组织ID
     * @param ugid 目标用户组ID
     * @param fid 分享文件夹ID（可选）
     * @param includeSub 是否包含子文件夹
     * @return 分享素材列表
     */
    List<MaterialShareRel> listSharedMaterials(Long oid, Long ugid, Long fid, boolean includeSub);

    /**
     * 按文件夹查询素材（自动判断用户组/公共文件夹）
     * @param oid 组织ID
     * @param fid 文件夹ID
     * @param includeSub 是否包含子文件夹
     * @return 素材列表
     */
    List<Material> listMaterialsByFolder(Long oid, Long fid, boolean includeSub);

    /**
     * 查询用户组可见的所有素材（自有+公共+分享）
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @return 素材列表
     */
    List<Material> listAllVisibleMaterials(Long oid, Long ugid);

    /**
     * 根据文件夹ID查询素材数量
     * @param fid 文件夹ID
     * @return 素材数量
     */
    long countMaterialsByFolder(Long fid);

    /**
     * 根据用户组ID查询素材数量
     * @param ugid 用户组ID
     * @return 素材数量
     */
    long countMaterialsByUserGroup(Long ugid);

    /**
     * 查询公共素材数量
     * @param oid 组织ID
     * @return 素材数量
     */
    long countPublicMaterials(Long oid);

    /**
     * 检查素材是否属于指定组织
     * @param oid 组织ID
     * @param mid 素材ID
     * @return 是否属于该组织
     */
    boolean isMaterialBelongsToOrg(Long oid, Long mid);

    /**
     * 检查素材是否属于指定用户组
     * @param ugid 用户组ID
     * @param mid 素材ID
     * @return 是否属于该用户组
     */
    boolean isMaterialBelongsToUserGroup(Long ugid, Long mid);

    /**
     * 根据分享记录ID查询分享素材详情
     * @param shareId 分享记录ID
     * @return 分享素材详情
     */
    MaterialShareRel getSharedMaterialById(Long shareId);

    /**
     * 检查素材是否被分享给指定用户组
     * @param mid 素材ID
     * @param ugid 用户组ID
     * @return 是否被分享
     */
    boolean isMaterialSharedToUserGroup(Long mid, Long ugid);

    /**
     * 根据文件ID查询素材
     * @param fileId 文件ID
     * @return 素材
     */
    Material getMaterialByFileId(String fileId);

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
}