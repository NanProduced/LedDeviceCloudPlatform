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
     * 获取指定组织的公共资源组根文件夹
     * @param oid 组织ID
     * @return 公共资源组根文件夹，如果不存在则返回null
     */
    Folder getPublicRootFolderByOrg(Long oid);

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
     * 根据父文件夹路径获取直接子文件夹（基于path字段优化）
     * @param parentPath 父文件夹路径
     * @return 直接子文件夹列表
     */
    List<Folder> getDirectChildFoldersByParentPath(String parentPath);

    /**
     * 获取分享给指定用户组的文件夹
     * @param ugid 用户组ID
     * @return 分享文件夹列表
     */
    List<Folder> getSharedFolders(Long ugid);

    /**
     * 获取指定文件夹及其所有子文件夹的ID列表（类似getAllUgidsByParent）
     * @param fid 父文件夹ID
     * @return 包含父文件夹及所有子文件夹的ID列表
     */
    List<Long> getAllFidsByParent(Long fid);

    /**
     * 获取指定文件夹的路径
     * @param fid 文件夹ID
     * @return 文件夹路径
     */
    String getPathByFid(Long fid);
}
