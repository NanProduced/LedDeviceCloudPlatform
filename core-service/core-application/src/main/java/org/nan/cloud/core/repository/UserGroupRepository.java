package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.UserGroup;

import java.util.List;

public interface UserGroupRepository {

    UserGroup createUserGroup(UserGroup userGroup);

    UserGroup getUserGroupById(Long ugid);

    List<UserGroup> getAllUserGroupsByParent(Long ugid);

    List<UserGroup> getDirectUserGroupsByParent(Long ugid);
}
