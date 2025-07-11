package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.UserGroup;

public interface UserGroupRepository {

    UserGroup createUserGroup(UserGroup userGroup);

    UserGroup getUserGroupById(Long ugid);
}
