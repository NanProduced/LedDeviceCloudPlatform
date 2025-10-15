package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.User;

import java.util.List;
import java.util.Set;

public interface UserRepository {

    User createUser(User user);
    User getUserById(Long uid);
    PageVO<User> pageUsers(int pageNum, int pageSize, Long oid, Set<Long> ugids, String usernameKeyword, String emailKeyword, Integer status);
    List<User> getUsersByUgids(Long oid, List<Long> ugids);
    void updateUser(User user);
    void updateUserStatus(Long uid, Integer status);
    void modifyUserGroup(Long uid, Long ugid);
    void deleteUser(Long uid);
    boolean ifHasSameUsername(Long oid, String username);
    boolean isAncestorOrSiblingByUser(Long aUgid, Long bUgid);
    boolean ifTheSameOrg(Long oid, Long targetUid);
}
