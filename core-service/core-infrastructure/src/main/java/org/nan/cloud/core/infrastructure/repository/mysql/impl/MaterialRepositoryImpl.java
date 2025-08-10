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
import org.nan.cloud.core.repository.FolderRepository;
import org.nan.cloud.core.repository.MaterialRepository;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MaterialRepositoryImpl implements MaterialRepository {

    private final MaterialMapper materialMapper;
    private final MaterialShareRelMapper materialShareRelMapper;
    private final MaterialConverter materialConverter;
    private final UserGroupRepository userGroupRepository;
    private final FolderRepository folderRepository;

    @Override
    public Material getMaterialById(Long mid) {
        MaterialDO materialDO = materialMapper.selectMaterialWithFileById(mid);
        return materialConverter.toMaterial(materialDO);
    }

    @Override
    public List<Material> listMaterialsByUserGroup(Long oid, Long ugid, Long fid, boolean includeSub) {
        // 🎯 正确的API调用逻辑：ugid和fid互斥，只传一个
        
        if (fid == null) {
            // 📋 用户组模式：查询用户组下的素材
            return handleUserGroupQuery(oid, ugid, includeSub);
        } else {
            // 📁 文件夹模式：查询文件夹下的素材
            return handleFolderQuery(oid, fid, includeSub);
        }
    }
    
    /**
     * 处理用户组查询模式
     */
    private List<Material> handleUserGroupQuery(Long oid, Long ugid, boolean includeSub) {
        List<Long> ugidList;
        Boolean includeNullFid;
        
        if (includeSub) {
            // 🎯 用户组展开：查询该组及所有子组的所有素材（根级+所有文件夹+所有子文件夹）
            ugidList = userGroupRepository.getAllUgidsByParent(ugid);
            includeNullFid = false; // 不限制文件夹，查询所有文件夹的素材
            log.debug("用户组展开模式 - 原组: {}, 展开后: {} (共{}个), 包含所有文件夹层次", 
                     ugid, ugidList, ugidList.size());
        } else {
            // 🎯 用户组根级：只查该用户组根级素材（fid IS NULL）
            ugidList = List.of(ugid);
            includeNullFid = true; // 只查询根级素材
            log.debug("用户组根级模式 - 用户组: {}, 只查询根级素材(fid=null)", ugid);
        }
        
        // 🚀 性能优化：空列表直接返回
        if (ugidList.isEmpty()) {
            log.warn("用户组列表为空，返回空结果 - ugid: {}", ugid);
            return new ArrayList<>();
        }
        
        List<MaterialDO> materialDOS = materialMapper.selectMaterialsByDualDimension(
            oid, ugidList, null, includeNullFid);
        
        log.debug("用户组查询结果: {} 条素材 - 组织: {}, 用户组数: {}, 包含根级: {}", 
                 materialDOS.size(), oid, ugidList.size(), includeNullFid);
        
        return materialConverter.toMaterials(materialDOS);
    }
    
    /**
     * 处理文件夹查询模式  
     */
    private List<Material> handleFolderQuery(Long oid, Long fid, boolean includeSub) {
        List<Long> fidList;
        
        if (includeSub) {
            // 🎯 文件夹展开：查询该文件夹及所有子文件夹素材
            fidList = folderRepository.getAllFidsByParent(fid);
            log.debug("文件夹展开模式 - 原文件夹: {}, 展开后: {} (共{}个)", 
                     fid, fidList, fidList.size());
        } else {
            // 🎯 文件夹精确：只查该文件夹素材
            fidList = List.of(fid);
            log.debug("文件夹精确模式 - 文件夹: {}", fid);
        }
        
        // 🚀 性能优化：空列表直接返回
        if (fidList.isEmpty()) {
            log.warn("文件夹列表为空，返回空结果 - fid: {}", fid);
            return new ArrayList<>();
        }
        
        // 🔧 查询指定文件夹的素材，不限制用户组（文件夹已确定范围）
        List<MaterialDO> materialDOS = materialMapper.selectMaterialsByDualDimension(
            oid, null, fidList, false);
        
        log.debug("文件夹查询结果: {} 条素材 - 组织: {}, 文件夹数: {}", 
                 materialDOS.size(), oid, fidList.size());
        
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
    public boolean isMaterialBelongsToOrg(Long oid, Long mid) {
        return materialMapper.exists(new LambdaQueryWrapper<MaterialDO>()
                .eq(MaterialDO::getMid, mid)
                .eq(MaterialDO::getOid, oid));
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

    @Override
    public List<Material> batchGetMaterialsByIds(List<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            log.debug("批量查询素材 - 素材ID列表为空，返回空结果");
            return new ArrayList<>();
        }
        
        // 🚀 性能优化：限制单次查询数量，避免SQL过长
        if (materialIds.size() > 1000) {
            log.warn("批量查询素材 - 查询数量过多: {}, 限制为1000个", materialIds.size());
            materialIds = materialIds.subList(0, 1000);
        }
        
        try {
            // 🔧 使用MyBatis Plus的selectBatchIds进行批量查询
            List<MaterialDO> materialDOS = materialMapper.selectByIds(materialIds);
            
            log.debug("批量查询素材完成 - 请求: {}, 返回: {}", materialIds.size(), materialDOS.size());
            
            // 🔄 转换为域对象
            return materialConverter.toMaterials(materialDOS);
            
        } catch (Exception e) {
            log.error("批量查询素材失败 - 素材IDs: {}, 错误: {}", materialIds, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public Set<Long> batchCheckBelongsToOrg(Long oid, List<Long> materialIds) {
        return materialMapper.selectList(new LambdaQueryWrapper<MaterialDO>()
                .select(MaterialDO::getMid)
                .eq(MaterialDO::getOid, oid)
                .in(MaterialDO::getMid, materialIds)).stream().map(MaterialDO::getMid).collect(Collectors.toSet());
    }
}