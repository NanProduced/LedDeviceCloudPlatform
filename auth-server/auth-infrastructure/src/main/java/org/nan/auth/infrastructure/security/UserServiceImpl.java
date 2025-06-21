package org.nan.auth.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.nan.auth.application.model.User;
import org.nan.auth.application.repository.UserRepository;
import org.nan.auth.common.utils.StringUtils;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.security.InvalidParameterException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserDetailsService, Serializable {

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

        final User user = userRepository.findUserByUsernameAndSuffix(str[0], Long.parseLong(str[1]));
        UserPrincipal userPrincipal = new UserPrincipal(user);
        if (!userPrincipal.isEnabled()) throw new DisabledException(String.format("User has been baned: [%s].", userPrincipal.getUsername()));

        return userPrincipal;
    }
}
