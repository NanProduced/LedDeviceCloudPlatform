package org.nan.auth.application.repository;

import org.nan.auth.application.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findUserByUsernameAndSuffix(String username, Long suffix);
}
