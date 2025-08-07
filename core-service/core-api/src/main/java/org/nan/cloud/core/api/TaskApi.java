package org.nan.cloud.core.api;

import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryTaskRequest;
import org.nan.cloud.core.api.DTO.res.QueryTaskResponse;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

public interface TaskApi {

    String prefix = "/task";

    @PostMapping(prefix + "/list")
    PageVO<QueryTaskResponse> getUserTask(PageRequestDTO<QueryTaskRequest> requestDTO);

    @PostMapping(prefix + "/count/status")
    Map<String, Long> getCountMapByStatus();
}
