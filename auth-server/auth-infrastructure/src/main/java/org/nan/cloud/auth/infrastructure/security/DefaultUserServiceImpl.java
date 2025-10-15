package org.nan.cloud.auth.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.auth.application.model.User;
import org.nan.cloud.auth.application.repository.UserRepository;
import org.nan.cloud.auth.common.utils.StringUtils;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultUserServiceImpl implements UserDetailsService, Serializable {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        String[] str = StringUtils.splitByHash(input);
        if (null == str) throw new InvalidParameterException(String.format("Invalid input: [%s]", input));

        try {
            Long.parseLong(str[1]);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(String.format("Invalid input: [%s]", input));
        }

        final Optional<User> userOptional = userRepository.findUserByUsernameAndSuffix(str[0], Long.parseLong(str[1]));
        User user = userOptional.orElseThrow(() ->
                new UsernameNotFoundException("User not found with username: " + str[0] + " and suffix: " + str[1])
        );
        UserPrincipal userPrincipal = new UserPrincipal(user);
        if (!userPrincipal.isEnabled()) throw new DisabledException(String.format("User has been baned: [%s].", userPrincipal.getUsername()));

        return userPrincipal;
    }
}
