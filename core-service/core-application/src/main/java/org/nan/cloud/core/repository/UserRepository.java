package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.User;

import java.util.Set;

public interface UserRepository {

    User createUser(User user);
    User getUserById(Long uid);
    PageVO<User> pageUsers(int pageNum, int pageSize, Long oid, Set<Long> ugids, String usernameKeyword, String emailKeyword);
    void updateUser(User user);
    void updateUserStatus(Long uid, Integer status);
    void modifyUserGroup(Long uid, Long ugid);
    void deleteUser(Long uid);
    boolean ifHasSameUsername(Long oid, String username);
    boolean isAncestorOrSiblingByUser(Long aUgid, Long bUgid);
}
