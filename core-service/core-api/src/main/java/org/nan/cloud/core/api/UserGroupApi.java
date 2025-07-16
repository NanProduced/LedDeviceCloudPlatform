package org.nan.cloud.core.api;

import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.CreateUserGroupRequest;
import org.nan.cloud.core.api.DTO.req.QueryUserListRequest;
import org.nan.cloud.core.api.DTO.res.UserGroupTreeResponse;
import org.nan.cloud.core.api.DTO.res.UserListResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserGroupApi {

    String prefix = "/user-group";

    @GetMapping(prefix + "/tree/init")
    UserGroupTreeResponse getUserGroupTree();

    @PostMapping(prefix + "/list")
    PageVO<UserListResponse> listUser(@Validated @RequestBody PageRequestDTO<QueryUserListRequest> requestDTO);

    @PostMapping(prefix + "/create")
    void createUserGroup(@Validated @RequestBody CreateUserGroupRequest createUserGroupRequest);

    @PostMapping(prefix + "/delete")
    void deleteUserGroup(@RequestParam("ugid") Long ugid);
}
