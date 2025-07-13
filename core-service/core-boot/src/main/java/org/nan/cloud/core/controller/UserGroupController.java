package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.res.UserGroupTreeResponse;
import org.nan.cloud.core.api.UserGroupApi;
import org.nan.cloud.core.facade.UserGroupFacade;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "UserGroup(用户组控制器)", description = "用户组相关的操作")
@RestController
@RequiredArgsConstructor
public class UserGroupController implements UserGroupApi {

    private final UserGroupFacade userGroupFacade;

    @Override
    public UserGroupTreeResponse getUserGroupTree() {
        return userGroupFacade.getUserGroupTree();
    }
}
