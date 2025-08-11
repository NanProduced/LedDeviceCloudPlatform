package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.domain.MaterialShareRel;

import java.util.List;

public interface MaterialRepository {

    /**
     * 根据素材ID查询素材
     */
    Material getMaterialById(Long mid);

    String getFileIdByMaterialId(Long mid);

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
     * 查询用户组可见的所有素材（自有+公共+分享）
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @return 素材列表
     */
    List<Material> listAllVisibleMaterials(Long oid, Long ugid);

    /**
     * 检查素材是否属于指定组织
     * @param oid 组织ID
     * @param mid 素材ID
     * @return 是否属于该组织
     */
    boolean isMaterialBelongsToOrg(Long oid, Long mid);


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

    // ========== 转码任务查询相关方法 ==========

    /**
     * 根据ID和用户查询素材（权限控制）
     * @param materialId 素材ID
     * @param uid 用户ID
     * @param oid 组织ID
     * @return 素材信息
     */
    Material findByIdAndUser(Long materialId, Long uid, Long oid);

    /**
     * 根据源素材ID查询转码后的素材列表
     * @param sourceMaterialId 源素材ID
     * @return 转码后的素材列表
     */
    List<Material> findBySourceMaterialId(Long sourceMaterialId);

    /**
     * 根据ID查询素材
     * @param materialId 素材ID
     * @return 素材信息
     */
    Material findById(Long materialId);

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
     * 批量根据ID查询素材
     * @param materialIds 素材ID列表
     * @return 素材列表
     */
    List<Material> batchGetMaterialsByIds(List<Long> materialIds);

    /**
     * 批量检查素材是否属于指定组织
     * @param oid 组织ID
     * @param materialIds 素材ID列表
     * @return 属于该组织的素材ID集合
     */
    java.util.Set<Long> batchCheckBelongsToOrg(Long oid, List<Long> materialIds);
}