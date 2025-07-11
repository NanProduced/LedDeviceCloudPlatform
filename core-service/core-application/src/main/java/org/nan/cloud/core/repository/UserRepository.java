package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.User;

public interface UserRepository {

    User createUser(User user);
    User getUserById(Long uid);
}
