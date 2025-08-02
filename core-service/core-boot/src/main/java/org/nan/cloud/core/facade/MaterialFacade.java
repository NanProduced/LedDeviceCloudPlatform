package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.api.DTO.res.ListMaterialResponse;
import org.nan.cloud.core.api.DTO.res.ListSharedMaterialResponse;
import org.nan.cloud.core.api.DTO.res.MaterialNodeTreeResponse;
import org.nan.cloud.core.aspect.SkipOrgManagerPermissionCheck;
import org.nan.cloud.core.service.MaterialService;
import org.nan.cloud.core.service.PermissionChecker;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialFacade {

    private final MaterialService materialService;
    private final PermissionChecker permissionChecker;

    /**
     * 初始化素材管理树形结构
     */
    @SkipOrgManagerPermissionCheck
    public MaterialNodeTreeResponse initMaterialStructTree() {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        
        log.debug("Initializing material structure tree for user: {}, org: {}, userGroup: {}",
                requestUser.getUid(), requestUser.getOid(), requestUser.getUgid());
        
        return materialService.buildMaterialStructTree(requestUser.getOid(), requestUser.getUgid());
    }

    /**
     * 查询所有可见素材
     */
    @SkipOrgManagerPermissionCheck
    public List<ListMaterialResponse> listAllVisibleMaterials() {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        
        log.info("Listing all visible materials for user: {}, org: {}, userGroup: {}", 
                requestUser.getUid(), requestUser.getOid(), requestUser.getUgid());
        
        return materialService.listAllVisibleMaterials(requestUser.getOid(), requestUser.getUgid());
    }

    /**
     * 查询用户组素材
     */
    @SkipOrgManagerPermissionCheck
    public List<ListMaterialResponse> listUserMaterials(Long ugid, Long fid, Boolean includeSub) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        
        // 权限检查：检查是否有权限访问目标用户组
        if (ugid != null && !permissionChecker.ifHasPermissionOnTargetUserGroup(requestUser.getUgid(), ugid)) {
            log.warn("User {} does not have permission to access materials in user group {}", 
                    requestUser.getUid(), ugid);
            throw new RuntimeException("没有权限访问该用户组的素材");
        }
        
        // 权限检查：检查是否有权限访问目标文件夹
        if (fid != null && !permissionChecker.ifHasPermissionOnTargetFolder(requestUser.getOid(), requestUser.getUgid(), fid)) {
            log.warn("User {} does not have permission to access folder {}", 
                    requestUser.getUid(), fid);
            throw new RuntimeException("没有权限访问该文件夹");
        }
        
        // 如果未指定用户组，使用当前用户组
        Long targetUgid = ugid != null ? ugid : requestUser.getUgid();
        boolean includeSubFolders = includeSub != null ? includeSub : false;
        
        log.info("Listing user materials for user: {}, targetUserGroup: {}, folder: {}, includeSub: {}", 
                requestUser.getUid(), targetUgid, fid, includeSubFolders);
        
        return materialService.listUserMaterials(requestUser.getOid(), targetUgid, fid, includeSubFolders);
    }

    /**
     * 查询公共素材
     */
    @SkipOrgManagerPermissionCheck
    public List<ListMaterialResponse> listPublicMaterials(Long fid, boolean includeSub) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        
        // 权限检查：检查是否有权限访问公共文件夹
        if (fid != null && !permissionChecker.ifHasPermissionOnTargetFolder(requestUser.getOid(), requestUser.getUgid(), fid)) {
            log.warn("User {} does not have permission to access public folder {}", 
                    requestUser.getUid(), fid);
            throw new RuntimeException("没有权限访问该公共文件夹");
        }
        
        log.info("Listing public materials for user: {}, folder: {}, includeSub: {}", 
                requestUser.getUid(), fid, includeSub);
        
        return materialService.listPublicMaterials(requestUser.getOid(), fid, includeSub);
    }

    /**
     * 查询分享素材
     */
    @SkipOrgManagerPermissionCheck
    public List<ListSharedMaterialResponse> listSharedMaterials(Long fid, boolean includeSub) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        
        // 权限检查：检查是否有权限访问分享文件夹
        if (fid != null && !permissionChecker.ifHasPermissionOnTargetFolder(requestUser.getOid(), requestUser.getUgid(), fid)) {
            log.warn("User {} does not have permission to access shared folder {}", 
                    requestUser.getUid(), fid);
            throw new RuntimeException("没有权限访问该分享文件夹");
        }
        
        log.info("Listing shared materials for user: {}, userGroup: {}, folder: {}, includeSub: {}", 
                requestUser.getUid(), requestUser.getUgid(), fid, includeSub);
        
        return materialService.listSharedMaterials(requestUser.getOid(), requestUser.getUgid(), fid, includeSub);
    }
}