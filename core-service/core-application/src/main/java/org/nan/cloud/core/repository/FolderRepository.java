package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Folder;

import java.util.List;

public interface FolderRepository {

    void createFolder(Folder folder);

    Folder getFolderById(Long fid);

    Long getFolderUgidByFid(Long fid);

    boolean isTheSameOrg(Long oid, Long fid);

    /**
     * 某个文件夹下是否有重名文件夹
     * @param targetFid
     * @param folderName
     * @return
     */
    boolean ifHasSameFolderNameInFolder(Long targetFid, String folderName);

    /**
     * 某个用户组下是否有重名根文件夹
     * @param targetUgid
     * @param folderName
     * @return
     */
    boolean ifHasSameFolderNameInUserGroup(Long targetUgid, String folderName);

    /**
     * 获取用户组下的根级文件夹
     * @param ugid 用户组ID，null表示公共资源组
     * @return 根级文件夹列表
     */
    List<Folder> getRootFoldersByUserGroup(Long ugid);

    /**
     * 获取指定父文件夹下的子文件夹
     * @param parentFid 父文件夹ID
     * @return 子文件夹列表
     */
    List<Folder> getChildFolders(Long parentFid);

    /**
     * 获取用户组下的所有文件夹（平铺列表）
     * @param ugid 用户组ID，null表示公共资源组
     * @return 文件夹列表
     */
    List<Folder> getAllFoldersByUserGroup(Long ugid);

    /**
     * 根据路径前缀查询子文件夹
     * @param pathPrefix 路径前缀
     * @return 子文件夹列表
     */
    List<Folder> getFoldersByPathPrefix(String pathPrefix);

    /**
     * 获取分享给指定用户组的文件夹
     * @param ugid 用户组ID
     * @return 分享文件夹列表
     */
    List<Folder> getSharedFolders(Long ugid);
}
