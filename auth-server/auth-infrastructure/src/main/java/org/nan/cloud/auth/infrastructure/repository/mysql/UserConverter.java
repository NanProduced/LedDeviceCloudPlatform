package org.nan.cloud.auth.infrastructure.repository.mysql;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.auth.application.model.User;
import org.nan.cloud.auth.infrastructure.repository.mysql.DO.UserDO;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface UserConverter {

    User userDO2User(UserDO userDO);


}
