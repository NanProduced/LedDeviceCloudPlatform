package org.nan.auth.infrastructure.repository.mysql;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.auth.application.model.User;
import org.nan.auth.infrastructure.repository.mysql.DO.UserDO;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface UserConverter {

    @Mapping(target = "UGid", source = "u_group_id")
    User userDO2User(UserDO userDO);


}
