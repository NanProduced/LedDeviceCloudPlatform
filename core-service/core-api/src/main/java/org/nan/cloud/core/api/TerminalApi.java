package org.nan.cloud.core.api;

import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.CreateTerminalRequest;
import org.nan.cloud.core.api.DTO.req.QueryTerminalListRequest;
import org.nan.cloud.core.api.DTO.res.TerminalListResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface TerminalApi {

    String prefix = "/terminal";

    @PostMapping(prefix + "/create")
    void createTerminal(@Validated @RequestBody CreateTerminalRequest createTerminalRequest);

    @PostMapping(prefix + "/list")
    PageVO<TerminalListResponse> listTerminals(@Validated @RequestBody PageRequestDTO<QueryTerminalListRequest> requestDTO);

}
