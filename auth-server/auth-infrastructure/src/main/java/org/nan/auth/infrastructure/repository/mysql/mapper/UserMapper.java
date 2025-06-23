package org.nan.auth.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.nan.auth.infrastructure.repository.mysql.DO.UserDO;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
