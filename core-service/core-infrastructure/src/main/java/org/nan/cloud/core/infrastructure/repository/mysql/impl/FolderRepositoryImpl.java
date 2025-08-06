package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.nan.cloud.core.domain.Folder;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.FolderDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.MaterialConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.FolderMapper;
import org.nan.cloud.core.repository.FolderRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FolderRepositoryImpl implements FolderRepository {

    private final MaterialConverter materialConverter;

    private final FolderMapper folderMapper;

    @Override
    public void createFolder(Folder folder) {
        LocalDateTime now = LocalDateTime.now();
        FolderDO folderDO = FolderDO.builder()
                .folderName(folder.getFolderName())
                .folderType("NORMAL")
                .description(folder.getDescription())
                .parent(folder.getParent())
                .ugid(folder.getUgid())
                .oid(folder.getOid())
                .creatorId(folder.getCreatorId())
                .updaterId(folder.getCreatorId())
                .createTime(now)
                .updateTime(now)
                .build();
        folderMapper.insert(folderDO);
        String path = StringUtils.isBlank(folderDO.getPath()) ? String.valueOf(folderDO.getFid()) : folderDO.getPath() + "|" + folderDO.getFid();
        folderDO.setPath(path);
        folderMapper.updateById(folderDO);
    }

    @Override
    public Folder getFolderById(Long fid) {
        return materialConverter.toFolder(folderMapper.selectById(fid));
    }

    @Override
    public Long getFolderUgidByFid(Long fid) {
        return folderMapper.selectOne(new LambdaQueryWrapper<FolderDO>()
        .eq(FolderDO::getFid, fid)).getUgid();
    }

    @Override
    public boolean isTheSameOrg(Long oid, Long fid) {
        return folderMapper.exists(new LambdaQueryWrapper<FolderDO>()
                .eq(FolderDO::getFid, fid)
                .eq(FolderDO::getOid, oid));
    }

    @Override
    public boolean ifHasSameFolderNameInFolder(Long targetFid, String folderName) {
        return folderMapper.exists(new LambdaQueryWrapper<FolderDO>()
                .eq(FolderDO::getParent, targetFid)
                .eq(FolderDO::getFolderName, folderName));
    }

    @Override
    public boolean ifHasSameFolderNameInUserGroup(Long targetUgid, String folderName) {
        return folderMapper.exists(new LambdaQueryWrapper<FolderDO>()
                .eq(FolderDO::getUgid, targetUgid)
                .isNull(FolderDO::getParent)
                .eq(FolderDO::getFolderName, folderName));
    }

    @Override
    public List<Folder> getRootFoldersByUserGroup(Long ugid) {
        LambdaQueryWrapper<FolderDO> queryWrapper = new LambdaQueryWrapper<FolderDO>()
                .isNull(FolderDO::getParent)
                .orderByAsc(FolderDO::getFolderName);
        
        if (ugid == null) {
            // 公共资源组
            queryWrapper.eq(FolderDO::getFolderType, "PUBLIC");
        } else {
            // 指定用户组
            queryWrapper.eq(FolderDO::getUgid, ugid)
                    .eq(FolderDO::getFolderType, "NORMAL");
        }
        
        List<FolderDO> folderDOS = folderMapper.selectList(queryWrapper);
        return materialConverter.toFolders(folderDOS);
    }

    @Override
    public List<Folder> getChildFolders(Long parentFid) {
        List<FolderDO> folderDOS = folderMapper.selectList(new LambdaQueryWrapper<FolderDO>()
                .eq(FolderDO::getParent, parentFid)
                .orderByAsc(FolderDO::getFolderName));
        return materialConverter.toFolders(folderDOS);
    }

    @Override
    public List<Folder> getAllFoldersByUserGroup(Long ugid) {
        LambdaQueryWrapper<FolderDO> queryWrapper = new LambdaQueryWrapper<FolderDO>()
                .orderByAsc(FolderDO::getPath);
        
        if (ugid == null) {
            // 公共资源组
            queryWrapper.eq(FolderDO::getFolderType, "PUBLIC");
        } else {
            // 指定用户组
            queryWrapper.eq(FolderDO::getUgid, ugid)
                    .eq(FolderDO::getFolderType, "NORMAL");
        }
        
        List<FolderDO> folderDOS = folderMapper.selectList(queryWrapper);
        return materialConverter.toFolders(folderDOS);
    }

    @Override
    public List<Folder> getFoldersByPathPrefix(String pathPrefix) {
        List<FolderDO> folderDOS = folderMapper.selectList(new LambdaQueryWrapper<FolderDO>()
                .likeRight(FolderDO::getPath, pathPrefix)
                .orderByAsc(FolderDO::getPath));
        return materialConverter.toFolders(folderDOS);
    }

    @Override
    public List<Folder> getSharedFolders(Long ugid) {
        // 这里需要连表查询material_share_rel表
        // 暂时返回空列表，后续可以通过自定义SQL实现
        return List.of();
    }
}
