package org.nan.cloud.core.api;

import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryUserListRequest;
import org.nan.cloud.core.api.DTO.res.UserGroupTreeResponse;
import org.nan.cloud.core.api.DTO.res.UserListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

public interface UserGroupApi {

    String prefix = "/user-group";

    @GetMapping(prefix + "/tree/init")
    UserGroupTreeResponse getUserGroupTree();

    @PostMapping(prefix + "/list")
    PageVO<UserListResponse> listUser(PageRequestDTO<QueryUserListRequest> requestDTO);



}
