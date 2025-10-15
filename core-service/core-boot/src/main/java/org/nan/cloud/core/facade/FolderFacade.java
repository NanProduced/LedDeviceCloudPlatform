package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.DTO.CreateFolderDTO;
import org.nan.cloud.core.api.DTO.req.CreateFolderRequest;
import org.nan.cloud.core.aspect.SkipOrgManagerPermissionCheck;
import org.nan.cloud.core.service.FolderService;
import org.nan.cloud.core.service.PermissionChecker;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class FolderFacade {

    private final PermissionChecker permissionChecker;

    private final FolderService folderService;


    @Transactional(rollbackFor =  Exception.class)
    @SkipOrgManagerPermissionCheck
    public void createFolder(CreateFolderRequest createFolderRequest){
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        if (Objects.nonNull(createFolderRequest.getFid())) {
            ExceptionEnum.FOLDER_PERMISSION_DENIED.throwIf(
                    !permissionChecker.ifHasPermissionOnTargetFolder(requestUser.getOid(), requestUser.getUgid(), createFolderRequest.getFid()));
            folderService.createFolderInFolder(CreateFolderDTO.builder()
                            .folderName(createFolderRequest.getFolderName())
                            .description(createFolderRequest.getDescription())
                            .oid(requestUser.getOid())
                            .targetFid(createFolderRequest.getFid())
                            .uid(requestUser.getUid())
                            .build());
        }
        else {
            ExceptionEnum.FOLDER_PERMISSION_DENIED.throwIf(
                    !permissionChecker.ifHasPermissionOnTargetUserGroup(requestUser.getUgid(), createFolderRequest.getUgid()));
            folderService.createFolderInUserGroup(CreateFolderDTO.builder()
                            .folderName(createFolderRequest.getFolderName())
                            .description(createFolderRequest.getDescription())
                            .oid(requestUser.getOid())
                            .targetUgid(createFolderRequest.getUgid())
                            .uid(requestUser.getUid())
                            .build());
        }

    }
}
