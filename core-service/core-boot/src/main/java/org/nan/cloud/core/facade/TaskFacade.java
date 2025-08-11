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
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * 任务状态计数
     */
    public Map<String, Long> countByStatus() {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        PageRequestDTO<QueryTaskRequest> req = new PageRequestDTO<>();
        req.setPageNum(1);
        req.setPageSize(1000);
        req.setParams(new QueryTaskRequest());
        PageVO<Task> page = taskService.listTasks(req, requestUser.getOid(), requestUser.getUid());
        Map<String, Long> counts = page.getRecords().stream()
                .collect(Collectors.groupingBy(t -> t.getTaskStatus() == null ? "UNKNOWN" : t.getTaskStatus().name(), Collectors.counting()));
        counts.put("TOTAL", page.getTotal());
        return counts;
    }

    public void cancelTask(String taskId) {
        taskService.cancelTask(taskId);
    }

    public void deleteTasks(List<String> taskIds) {
        taskService.deleteTasks(taskIds);
    }

}
