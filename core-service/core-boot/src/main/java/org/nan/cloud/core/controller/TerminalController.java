package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.CreateTerminalRequest;
import org.nan.cloud.core.api.DTO.req.QueryTerminalListRequest;
import org.nan.cloud.core.api.DTO.res.TerminalListResponse;
import org.nan.cloud.core.api.TerminalApi;
import org.nan.cloud.core.facade.TerminalFacade;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Terminal(终端控制器)", description = "终端相关的所有操作")
@RestController
@RequiredArgsConstructor
public class TerminalController implements TerminalApi {

    private final TerminalFacade  terminalFacade;

    @Operation(
            summary = "创建终端",
            description = "创建新的终端账号",
            tags = {"终端管理"}
    )
    @Override
    public void createTerminal(CreateTerminalRequest createTerminalRequest) {
        terminalFacade.createTerminal(createTerminalRequest);
    }

    @Operation(
            summary = "查询终端列表",
            description = "根据终端组ID分页查询终端列表，支持关键字搜索和终端型号筛选",
            tags = {"终端管理"}
    )
    @Override
    public PageVO<TerminalListResponse> listTerminals(PageRequestDTO<QueryTerminalListRequest> requestDTO) {
        return terminalFacade.listTerminals(requestDTO);
    }
}
