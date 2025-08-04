package org.nan.cloud.core.infrastructure.repository.mysql.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TaskDO;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface TaskConverter {

    TaskDO toTaskDO(Task task);

    @Mapping(target = "progress", ignore = true) // progress字段是领域字段，不从数据库读取
    @Mapping(target = "creatorName", ignore = true) // creatorName字段是领域字段，不从数据库读取
    Task toTask(TaskDO taskDO);

}
