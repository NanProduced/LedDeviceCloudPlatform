package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
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

@Repository
@RequiredArgsConstructor
public class MaterialRepositoryImpl implements MaterialRepository {

    private final MaterialMapper materialMapper;
    private final MaterialShareRelMapper materialShareRelMapper;
    private final MaterialConverter materialConverter;

    @Override
    public Material getMaterialById(Long mid) {
        MaterialDO materialDO = materialMapper.selectMaterialWithFileById(mid);
        return materialConverter.toMaterial(materialDO);
    }

    @Override
    public List<Material> listMaterialsByUserGroup(Long oid, Long ugid, Long fid, boolean includeSub) {
        String fidCondition = buildFidCondition(fid, includeSub);
        List<MaterialDO> materialDOS = materialMapper.selectMaterialsByUserGroup(oid, ugid, fidCondition);
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
        List<MaterialDO> materialDOS = materialMapper.selectAllVisibleMaterials(oid, ugid);
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