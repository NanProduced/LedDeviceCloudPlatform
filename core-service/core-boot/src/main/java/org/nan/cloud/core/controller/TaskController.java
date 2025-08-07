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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "任务管理器", description = "查询任务相关列表及信息")
@Slf4j
@RestController
@RequiredArgsConstructor
public class TaskController implements TaskApi {

    @Operation(
            summary = "获取用户任务列表",
            description = "分页查询当前用户的任务列表，支持类型筛选和任务状态筛选",
            tags = {"任务管理", "通用接口"}
    )
    @Override
    public PageVO<QueryTaskResponse> getUserTask(PageRequestDTO<QueryTaskRequest> requestDTO) {
        return null;
    }

    @Operation(
            summary = "获取用户任务分类统计",
            description = "根据任务状态对用户任务数量进行统计",
            tags = {"任务管理", "通用接口"}
    )
    @Override
    public Map<String, Long> getCountMapByStatus() {
        return Map.of();
    }
}
