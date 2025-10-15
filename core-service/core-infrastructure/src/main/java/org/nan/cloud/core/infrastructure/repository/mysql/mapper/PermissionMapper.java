package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.PermissionDO;

import java.util.List;
import java.util.Set;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionDO> {

}
