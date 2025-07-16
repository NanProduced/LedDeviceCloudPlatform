package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.CreateUserGroupDTO;
import org.nan.cloud.core.DTO.UserGroupRelDTO;
import org.nan.cloud.core.domain.UserGroup;

import java.util.List;

public interface UserGroupService {

    UserGroup getUserGroupById(Long ugid);

    void createUserGroup(CreateUserGroupDTO createUserGroupDTO);

    void deleteUserGroup(Long oid, Long ugid);
    /**
     * 查询组下所有用户组（包含当前组）
     * @param ugid
     * @return
     */
    List<UserGroupRelDTO> getAllUserGroupsByParent(Long ugid);
}
