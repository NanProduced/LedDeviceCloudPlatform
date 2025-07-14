package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.User;

public interface UserRepository {

    User createUser(User user);
    User getUserById(Long uid);
    void updateUser(User user);
    void updateUserStatus(Long uid, Integer status);
    void modifyUserGroup(Long uid, Long ugid);
    void deleteUser(Long uid);
    boolean ifHasSameUsername(Long oid, String username);
    boolean isAncestorOrSiblingByUser(Long aUgid, Long bUgid);
}
