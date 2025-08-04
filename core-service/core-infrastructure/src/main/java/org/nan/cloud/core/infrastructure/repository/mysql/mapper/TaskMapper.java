package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TaskDO;

@Mapper
public interface TaskMapper extends BaseMapper<TaskDO> {
}
