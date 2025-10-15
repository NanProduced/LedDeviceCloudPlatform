package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.CreateUserGroupRequest;
import org.nan.cloud.core.api.DTO.req.QueryUserListRequest;
import org.nan.cloud.core.api.DTO.res.UserGroupTreeResponse;
import org.nan.cloud.core.api.DTO.res.UserListResponse;
import org.nan.cloud.core.api.UserGroupApi;
import org.nan.cloud.core.facade.UserGroupFacade;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "UserGroup(用户组控制器)", description = "用户组相关的操作")
@RestController
@RequiredArgsConstructor
public class UserGroupController implements UserGroupApi {

    private final UserGroupFacade userGroupFacade;

    @Operation(
            summary = "获取用户组树",
            description = "加载当前登录用户的用户组树",
            tags = {"用户组管理"}
    )
    @Override
    public UserGroupTreeResponse getUserGroupTree() {
        return userGroupFacade.getUserGroupTree();
    }

    @Operation(
            summary = "获取用户列表",
            description = "根据用户组Id获取用户列表",
            tags = {"用户组管理"}
    )
    @Override
    public PageVO<UserListResponse> listUser(PageRequestDTO<QueryUserListRequest> requestDTO) {
        return userGroupFacade.listUser(requestDTO);
    }

    @Operation(
            summary = "创建用户组",
            description = "在指定组下创建用户组",
            tags = {"用户组管理"}
    )
    @Override
    public void createUserGroup(CreateUserGroupRequest createUserGroupRequest) {
        userGroupFacade.createUserGroup(createUserGroupRequest);
    }

    @Operation(
            summary = "删除用户组",
            description = "删除指定用户组及其根组，需要组下没有用户",
            tags = {"用户组管理"}
    )
    @Override
    public void deleteUserGroup(Long ugid) {
        userGroupFacade.deleteUserGroup(ugid);
    }
}
