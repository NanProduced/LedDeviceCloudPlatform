package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.domain.MaterialShareRel;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialShareRelDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.MaterialConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.MaterialMapper;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.MaterialShareRelMapper;
import org.nan.cloud.core.repository.MaterialRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MaterialRepositoryImpl implements MaterialRepository {

    private final MaterialMapper materialMapper;
    private final MaterialShareRelMapper materialShareRelMapper;
    private final MaterialConverter materialConverter;
    // 🚀 新增UserGroupRepository依赖用于获取子组
    private final org.nan.cloud.core.repository.UserGroupRepository userGroupRepository;

    @Override
    public Material getMaterialById(Long mid) {
        MaterialDO materialDO = materialMapper.selectMaterialWithFileById(mid);
        return materialConverter.toMaterial(materialDO);
    }

    @Override
    public List<Material> listMaterialsByUserGroup(Long oid, Long ugid, Long fid, boolean includeSub) {
        String fidCondition = buildFidCondition(fid, includeSub);
        
        // 🔧 核心修复：支持子用户组查询
        List<Long> ugidList;
        if (includeSub) {
            // 获取包含子组的用户组ID列表（包含自身）
            ugidList = userGroupRepository.getAllUgidsByParent(ugid);
            log.debug("查询用户组及子组素材 - 原组: {}, 包含子组: {} (共{}个)", ugid, ugidList, ugidList.size());
        } else {
            // 仅查询当前用户组
            ugidList = List.of(ugid);
            log.debug("查询用户组素材 - 仅当前组: {}", ugid);
        }
        
        // 🚀 性能优化：空列表直接返回
        if (ugidList.isEmpty()) {
            log.warn("用户组列表为空，返回空结果 - ugid: {}", ugid);
            return new ArrayList<>();
        }
        
        List<MaterialDO> materialDOS = materialMapper.selectMaterialsByUserGroupList(oid, ugidList, fidCondition);
        log.debug("查询到素材数量: {} - 组织: {}, 用户组数: {}", materialDOS.size(), oid, ugidList.size());
        
        return materialConverter.toMaterials(materialDOS);
    }

    @Override
    public List<Material> listPublicMaterials(Long oid, Long fid, boolean includeSub) {
        String fidCondition = buildFidCondition(fid, includeSub);
        List<MaterialDO> materialDOS = materialMapper.selectPublicMaterials(oid, fidCondition);
        return materialConverter.toMaterials(materialDOS);
    }

    @Override
    public List<MaterialShareRel> listSharedMaterials(Long oid, Long ugid, Long fid, boolean includeSub) {
        String fidCondition = buildSharedFidCondition(fid, includeSub);
        List<MaterialShareRelDO> shareRelDOS = materialShareRelMapper.selectSharedMaterials(ugid, fidCondition);
        return materialConverter.toMaterialShareRels(shareRelDOS);
    }

    @Override
    public List<Material> listAllVisibleMaterials(Long oid, Long ugid) {
        // 🔧 核心修复：默认包含子组权限的全量素材查询
        List<Long> ugidList = userGroupRepository.getAllUgidsByParent(ugid);
        log.debug("查询全部可见素材 - 用户组: {}, 包含子组: {} (共{}个)", ugid, ugidList, ugidList.size());
        
        // 🚀 性能优化：空列表处理
        if (ugidList.isEmpty()) {
            log.warn("用户组列表为空，仅查询公共素材 - ugid: {}", ugid);
            return materialConverter.toMaterials(materialMapper.selectPublicMaterials(oid, ""));
        }
        
        List<MaterialDO> materialDOS = materialMapper.selectAllVisibleMaterialsByUserGroupList(oid, ugidList);
        log.debug("查询到全部可见素材数量: {} - 组织: {}, 用户组数: {}", materialDOS.size(), oid, ugidList.size());
        
        return materialConverter.toMaterials(materialDOS);
    }

    @Override
    public long countMaterialsByFolder(Long fid) {
        return materialMapper.countByFolder(fid);
    }

    @Override
    public long countMaterialsByUserGroup(Long ugid) {
        return materialMapper.countByUserGroup(ugid);
    }

    @Override
    public long countPublicMaterials(Long oid) {
        return materialMapper.countPublicMaterials(oid);
    }

    @Override
    public boolean isMaterialBelongsToOrg(Long oid, Long mid) {
        return materialMapper.exists(new LambdaQueryWrapper<MaterialDO>()
                .eq(MaterialDO::getMid, mid)
                .eq(MaterialDO::getOid, oid));
    }

    @Override
    public boolean isMaterialBelongsToUserGroup(Long ugid, Long mid) {
        return materialMapper.exists(new LambdaQueryWrapper<MaterialDO>()
                .eq(MaterialDO::getMid, mid)
                .eq(MaterialDO::getUgid, ugid));
    }

    @Override
    public MaterialShareRel getSharedMaterialById(Long shareId) {
        MaterialShareRelDO shareRelDO = materialShareRelMapper.selectSharedMaterialDetailById(shareId);
        return materialConverter.toMaterialShareRel(shareRelDO);
    }

    @Override
    public boolean isMaterialSharedToUserGroup(Long mid, Long ugid) {
        return materialShareRelMapper.existsSharedMaterialToUserGroup(mid, ugid);
    }

    @Override
    public Material getMaterialByFileId(String fileId) {
        MaterialDO materialDO = materialMapper.selectMaterialWithFileByFileId(fileId);
        return materialConverter.toMaterial(materialDO);
    }

    @Override
    public void createMaterial(Material material) {
        MaterialDO materialDO = materialConverter.toMaterialDO(material);
        LocalDateTime now = LocalDateTime.now();
        materialDO.setCreateTime(now);
        materialDO.setUpdateTime(now);
        materialMapper.insert(materialDO);
        material.setMid(materialDO.getMid());
    }

    @Override
    public void updateMaterial(Material material) {
        MaterialDO materialDO = materialConverter.toMaterialDO(material);
        materialDO.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(materialDO);
    }

    @Override
    public void deleteMaterial(Long mid) {
        materialMapper.deleteById(mid);
    }

    @Override
    public void deleteMaterials(List<Long> mids) {
        if (mids != null && !mids.isEmpty()) {
            materialMapper.deleteBatchIds(mids);
        }
    }

    /**
     * 构建文件夹条件SQL
     */
    private String buildFidCondition(Long fid, boolean includeSub) {
        if (fid == null) {
            return "AND m.fid IS NULL";
        }

        if (!includeSub) {
            return "AND m.fid = " + fid;
        }

        // 包含子文件夹：需要查询路径包含当前文件夹的所有文件夹
        return String.format("""
                AND (m.fid = %d OR m.fid IN (
                    SELECT f.fid FROM folder f 
                    WHERE f.path LIKE CONCAT((SELECT path FROM folder WHERE fid = %d), '|%%')
                ))
                """, fid, fid);
    }

    /**
     * 构建分享文件夹条件SQL
     */
    private String buildSharedFidCondition(Long fid, boolean includeSub) {
        if (fid == null) {
            return "";
        }

        if (!includeSub) {
            return "AND m.fid = " + fid;
        }

        // 分享素材的子文件夹查询
        return String.format("""
                AND (m.fid = %d OR m.fid IN (
                    SELECT f.fid FROM folder f 
                    WHERE f.path LIKE CONCAT((SELECT path FROM folder WHERE fid = %d), '|%%')
                ))
                """, fid, fid);
    }
}