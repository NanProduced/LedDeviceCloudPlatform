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

    @PostMapping(prefix + "/list")
    PageVO<TerminalGroupListResponse> listTerminalGroup(@Validated @RequestBody PageRequestDTO<QueryTerminalGroupListRequest> requestDTO);

    @PostMapping(prefix + "/search")
    PageVO<TerminalGroupListResponse> searchTerminalGroup(@Validated @RequestBody PageRequestDTO<SearchTerminalGroupRequest> requestDTO);

    @GetMapping(prefix + "/detail")
    TerminalGroupDetailResponse getTerminalGroupDetail(@RequestParam("tgid") Long tgid);

    @GetMapping(prefix + "/children")
    List<TerminalGroupDetailResponse> getChildrenTerminalGroups(@RequestParam("tgid") Long tgid);

    @PostMapping(prefix + "/user-group/bind")
    void bindUserGroupToTerminalGroup(@Validated @RequestBody BindUserGroupRequest bindUserGroupRequest);

    @PostMapping(prefix + "/user-group/unbind")
    void unbindUserGroupFromTerminalGroup(@RequestParam("tgid") Long tgid, @RequestParam("ugid") Long ugid);

    @PostMapping(prefix + "/user-group/list")
    PageVO<UserGroupBindingResponse> getTerminalGroupBindings(@Validated @RequestBody PageRequestDTO<QueryUserGroupBindingRequest> requestDTO);

    @GetMapping(prefix + "/accessible")
    List<TerminalGroupDetailResponse> getAccessibleTerminalGroups();

    @PostMapping(prefix + "/permission/check")
    TerminalGroupPermissionResponse checkTerminalGroupPermission(@RequestParam("tgid") Long tgid);

    @PostMapping(prefix + "/permission/batch")
    List<TerminalGroupPermissionResponse> batchCheckPermissions(@Validated @RequestBody BatchPermissionCheckRequest batchPermissionCheckRequest);

    @GetMapping(prefix + "/statistics")
    TerminalGroupStatisticsResponse getTerminalGroupStatistics(@RequestParam("tgid") Long tgid);

    @PostMapping(prefix + "/batch")
    BatchOperationResponse batchOperateTerminalGroups(@Validated @RequestBody BatchTerminalGroupOperationRequest batchOperationRequest);

    @PostMapping(prefix + "/history")
    PageVO<TerminalGroupHistoryResponse> getTerminalGroupHistory(@Validated @RequestBody PageRequestDTO<QueryTerminalGroupHistoryRequest> requestDTO);
}