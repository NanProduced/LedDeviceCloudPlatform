package org.nan.cloud.core.service.converter;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.api.DTO.res.QueryTaskResponse;
import org.nan.cloud.core.domain.Task;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface TaskDtoConverter {

    QueryTaskResponse toQueryTaskResponse(Task task);
    List<QueryTaskResponse> toQueryTaskResponseList(List<Task> tasks);
}
