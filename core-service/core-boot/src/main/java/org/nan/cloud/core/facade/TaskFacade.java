package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.api.DTO.req.QueryTaskRequest;
import org.nan.cloud.core.api.DTO.res.QueryTaskResponse;
import org.nan.cloud.core.service.converter.TaskDtoConverter;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.service.TaskService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskFacade {

    private final TaskService taskService;
    private final TaskDtoConverter taskConverter;

    /**
     * 查用户任务列表
     * @param pageRequestDTO
     * @return
     */
    public PageVO<QueryTaskResponse> listTasks(PageRequestDTO<QueryTaskRequest> pageRequestDTO) {

        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();

        PageVO<Task> taskPageVO = taskService.listTasks(pageRequestDTO, requestUser.getOid(), requestUser.getUid());

        List<QueryTaskResponse> queryTaskResponseList = taskConverter.toQueryTaskResponseList(taskPageVO.getRecords());

        return taskPageVO.withRecords(queryTaskResponseList);

    }


}
