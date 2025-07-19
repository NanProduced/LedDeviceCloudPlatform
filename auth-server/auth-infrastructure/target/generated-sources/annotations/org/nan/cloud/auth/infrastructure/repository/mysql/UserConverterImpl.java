package org.nan.cloud.auth.infrastructure.repository.mysql;

import javax.annotation.processing.Generated;
import org.nan.cloud.auth.application.model.User;
import org.nan.cloud.auth.infrastructure.repository.mysql.DO.UserDO;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-20T02:31:53+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.15 (Eclipse Adoptium)"
)
@Component
public class UserConverterImpl implements UserConverter {

    @Override
    public User userDO2User(UserDO userDO) {
        if ( userDO == null ) {
            return null;
        }

        User user = new User();

        if ( userDO.getUid() != null ) {
            user.setUid( userDO.getUid() );
        }
        if ( userDO.getOid() != null ) {
            user.setOid( userDO.getOid() );
        }
        if ( userDO.getUsername() != null ) {
            user.setUsername( userDO.getUsername() );
        }
        if ( userDO.getPassword() != null ) {
            user.setPassword( userDO.getPassword() );
        }
        if ( userDO.getUgid() != null ) {
            user.setUgid( userDO.getUgid() );
        }
        if ( userDO.getSuffix() != null ) {
            user.setSuffix( userDO.getSuffix() );
        }
        if ( userDO.getEmail() != null ) {
            user.setEmail( userDO.getEmail() );
        }
        if ( userDO.getPhone() != null ) {
            user.setPhone( userDO.getPhone() );
        }
        if ( userDO.getStatus() != null ) {
            user.setStatus( userDO.getStatus() );
        }
        if ( userDO.getUserType() != null ) {
            user.setUserType( userDO.getUserType() );
        }

        return user;
    }
}
