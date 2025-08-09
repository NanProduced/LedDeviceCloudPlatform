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
    // 🚀 新增FolderRepository依赖用于获取子文件夹  
    private final org.nan.cloud.core.repository.FolderRepository folderRepository;

    @Override
    public Material getMaterialById(Long mid) {
        MaterialDO materialDO = materialMapper.selectMaterialWithFileById(mid);
        return materialConverter.toMaterial(materialDO);
    }

    @Override
    public List<Material> listMaterialsByUserGroup(Long oid, Long ugid, Long fid, boolean includeSub) {
        // 🎯 正确的双维度查询策略
        
        List<Long> ugidList;
        List<Long> fidList = null;
        Boolean includeNullFid = false;
        
        if (includeSub) {
            // 👥 用户组展开模式：查询所有子组下的所有素材（包括所有文件夹）
            ugidList = userGroupRepository.getAllUgidsByParent(ugid);
            // 🔓 关键修复：不设置任何文件夹限制，查询所有素材
            fidList = null;          // 不限制文件夹
            includeNullFid = false;  // 不需要特殊处理NULL，因为没有文件夹限制
            log.debug("用户组展开模式 - 原组: {}, 展开后: {} (共{}个), 查询所有文件夹的素材", 
                     ugid, ugidList, ugidList.size());
        } else {
            // 📁 精确模式：限定用户组范围，应用文件夹过滤
            ugidList = List.of(ugid);
            
            if (fid != null) {
                // 🎯 精确文件夹查询
                fidList = List.of(fid);
                includeNullFid = false;
                log.debug("精确模式 - 用户组: {}, 指定文件夹: {}", ugid, fid);
            } else {
                // 🔓 查询该用户组下fid=null的素材
                fidList = null;
                includeNullFid = true;
                log.debug("精确模式 - 用户组: {}, 查询根级素材(fid=null)", ugid);
            }
        }
        
        // 🚀 性能优化：空列表直接返回
        if (ugidList.isEmpty()) {
            log.warn("用户组列表为空，返回空结果 - ugid: {}", ugid);
            return new ArrayList<>();
        }
        
        // 🔧 使用双维度查询方法
        List<MaterialDO> materialDOS = materialMapper.selectMaterialsByDualDimension(
            oid, ugidList, fidList, includeNullFid);
        
        log.debug("查询到素材数量: {} - 组织: {}, 用户组数: {}, 文件夹限制: {}", 
                 materialDOS.size(), oid, ugidList.size(), 
                 fidList != null ? "指定文件夹" : includeNullFid ? "仅根级" : "无限制");
        
        return materialConverter.toMaterials(materialDOS);
    }

    @Override
    public List<Material> listPublicMaterials(Long oid, Long fid, boolean includeSub) {
        // 📂 公共素材查询：文件夹主导的includeSub逻辑
        
        List<Long> fidList = null;
        Boolean includeNullFid = false;
        
        if (fid == null) {
            // 🔓 查询所有根级公共素材（fid=null）
            includeNullFid = true;
            log.debug("公共素材查询 - 所有根级素材(fid=null)");
        } else if (includeSub) {
            // 📁 文件夹展开模式：包含指定文件夹及其所有子文件夹
            fidList = folderRepository.getAllFidsByParent(fid);
            log.debug("公共素材查询 - 文件夹展开模式，原文件夹: {}, 展开后: {} (共{}个)", 
                     fid, fidList, fidList.size());
        } else {
            // 🎯 精确文件夹查询
            fidList = List.of(fid);
            log.debug("公共素材查询 - 精确文件夹: {}", fid);
        }
        
        // 🔧 使用双维度查询方法（ugidList=null表示查询公共素材）
        List<MaterialDO> materialDOS = materialMapper.selectMaterialsByDualDimension(
            oid, null, fidList, includeNullFid);
        
        log.debug("查询到公共素材数量: {} - 组织: {}, 文件夹过滤: {}", 
                 materialDOS.size(), oid, fidList != null ? fidList.size() : "无");
        
        return materialConverter.toMaterials(materialDOS);
    }

    @Override
    public List<MaterialShareRel> listSharedMaterials(Long oid, Long ugid, Long fid, boolean includeSub) {
        String fidCondition = buildSharedFidCondition(fid, includeSub);
        List<MaterialShareRelDO> shareRelDOS = materialShareRelMapper.selectSharedMaterials(ugid, fidCondition);
        return materialConverter.toMaterialShareRels(shareRelDOS);
    }

    @Override
    public List<Material> listMaterialsByFolder(Long oid, Long fid, boolean includeSub) {
        // 🎯 文件夹主导查询：纯文件夹层次展开，不涉及用户组展开
        
        List<Long> fidList;
        Boolean includeNullFid = false;
        
        if (includeSub) {
            // 📁 文件夹展开模式：包含该文件夹及其所有子文件夹
            fidList = folderRepository.getAllFidsByParent(fid);
            log.debug("文件夹展开模式 - 原文件夹: {}, 展开后: {} (共{}个)", fid, fidList, fidList.size());
        } else {
            // 🎯 精确文件夹查询
            fidList = List.of(fid);
            log.debug("精确文件夹模式 - 文件夹: {}", fid);
        }
        
        // 🚀 性能优化：空列表直接返回
        if (fidList.isEmpty()) {
            log.warn("文件夹列表为空，返回空结果 - fid: {}", fid);
            return new ArrayList<>();
        }
        
        // 🔧 查询指定文件夹的素材（不限制用户组，因为文件夹可能属于用户组或公共组）
        List<MaterialDO> materialDOS = materialMapper.selectMaterialsByDualDimension(
            oid, null, fidList, includeNullFid);
        
        log.debug("按文件夹查询到素材数量: {} - 组织: {}, 文件夹数: {}", 
                 materialDOS.size(), oid, fidList.size());
        
        return materialConverter.toMaterials(materialDOS);
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