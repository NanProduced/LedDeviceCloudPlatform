package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryTaskRequest;
import org.nan.cloud.core.api.DTO.res.QueryTaskResponse;
import org.nan.cloud.core.api.TaskApi;
import org.nan.cloud.core.facade.TaskFacade;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@Tag(name = "任务管理器", description = "查询任务相关列表及信息")
@Slf4j
@RestController
@RequiredArgsConstructor
public class TaskController implements TaskApi {

    private final TaskFacade taskFacade;

    @Operation(
            summary = "获取用户任务列表",
            description = "分页查询当前用户的任务列表，支持类型筛选和任务状态筛选",
            tags = {"任务管理", "通用接口"}
    )
    @Override
    public PageVO<QueryTaskResponse> getUserTask(PageRequestDTO<QueryTaskRequest> requestDTO) {
        return taskFacade.listTasks(requestDTO);
    }

    @Operation(summary = "获取用户任务分类统计", description = "根据任务状态统计：PENDING/RUNNING/COMPLETED/FAILED/CANCELED")
    @Override
    public Map<String, Long> getCountMapByStatus() {
        return taskFacade.countByStatus();
    }

    @Operation(summary = "取消任务")
    @Override
    public void cancelTask(String taskId) {
        taskFacade.cancelTask(taskId);
    }

    @Operation(summary = "批量删除任务")
    @Override
    public void deleteTasks(List<String> taskIds) {
        taskFacade.deleteTasks(taskIds);
    }
}
