package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.UserGroup;

import java.util.List;

public interface UserGroupRepository {

    UserGroup createUserGroup(UserGroup userGroup);

    UserGroup getUserGroupById(Long ugid);

    void deleteUserGroupsByUgids(List<Long> ugids);

    List<UserGroup> getAllUserGroupsByParent(Long ugid);

    List<Long> getAllUgidsByParent(Long ugid);

    List<UserGroup> getDirectUserGroupsByParent(Long ugid);

    boolean isAncestor(Long aUgid, Long bUgid);

    boolean isSibling(Long aUgid, Long bUgid);

    boolean isAncestorOrSibling(Long aUgid, Long bUgid);
}
