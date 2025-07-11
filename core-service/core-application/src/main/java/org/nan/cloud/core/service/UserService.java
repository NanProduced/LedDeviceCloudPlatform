package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.domain.User;

public interface UserService {

    User createOrgManagerUser(CreateOrgDTO dto);
    
    User getUserById(Long uid);
}
