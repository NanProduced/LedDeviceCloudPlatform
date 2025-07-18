package org.nan.cloud.core.api;

import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.*;
import org.nan.cloud.core.api.DTO.res.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface TerminalGroupApi {

    String prefix = "/terminal-group";

    @GetMapping(prefix + "/tree/init")
    TerminalGroupTreeResponse getTerminalGroupTree();

    @PostMapping(prefix + "/create")
    void createTerminalGroup(@Validated @RequestBody CreateTerminalGroupRequest createTerminalGroupRequest);

    @PostMapping(prefix + "/delete")
    void deleteTerminalGroup(@RequestParam("tgid") Long tgid);

    @PostMapping(prefix + "/update")
    void updateTerminalGroup(@Validated @RequestBody UpdateTerminalGroupRequest updateTerminalGroupRequest);

    @PostMapping(prefix + "/search")
    PageVO<TerminalGroupListResponse> searchTerminalGroup(@Validated @RequestBody PageRequestDTO<SearchTerminalGroupRequest> requestDTO);

    @GetMapping(prefix + "/detail")
    TerminalGroupDetailResponse getTerminalGroupDetail(@RequestParam("tgid") Long tgid);

    @PostMapping(prefix + "/bind/batch-update")
    BatchBindingOperationResponse updateUserGroupPermissions(@Validated @RequestBody BatchBindingOperationRequest request);

}