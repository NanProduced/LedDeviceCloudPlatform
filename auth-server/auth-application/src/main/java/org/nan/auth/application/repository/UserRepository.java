package org.nan.auth.application.repository;

import org.nan.auth.application.model.User;

public interface UserRepository {

    User findUserByUsernameAndSuffix(String username, Long suffix);
}
