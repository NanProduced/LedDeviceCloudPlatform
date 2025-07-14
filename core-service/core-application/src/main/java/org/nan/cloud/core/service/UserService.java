package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.DTO.CreateUserDTO;
import org.nan.cloud.core.domain.User;

public interface UserService {

    User createOrgManagerUser(CreateOrgDTO dto);
    
    User getUserById(Long uid);

    /**
     * 创建用户
     * @param dto
     * @return 新用户id
     */
    Long createUser(CreateUserDTO dto);

    void updateUser(User user);

    void inactiveUser(Long uid);

    void activeUser(Long uid);

    void moveUser(Long uid, Long targetUid);

    void deleteUser(Long uid);
}
