package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.*;
import org.nan.cloud.core.api.DTO.res.*;
import org.nan.cloud.core.api.TerminalGroupApi;
import org.nan.cloud.core.facade.TerminalGroupFacade;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "TerminalGroup(终端组控制器)", description = "终端组相关的操作")
@RestController
@RequiredArgsConstructor
public class TerminalGroupController implements TerminalGroupApi {

    private final TerminalGroupFacade terminalGroupFacade;

    @Operation(
            summary = "获取终端组树",
            description = "加载当前登录用户的终端组树",
            tags = {"终端组管理"}
    )
    @Override
    public TerminalGroupTreeResponse getTerminalGroupTree() {
        return terminalGroupFacade.getTerminalGroupTree();
    }

    @Operation(
            summary = "创建终端组",
            description = "在指定组下创建终端组",
            tags = {"终端组管理"}
    )
    @Override
    public void createTerminalGroup(CreateTerminalGroupRequest createTerminalGroupRequest) {
        terminalGroupFacade.createTerminalGroup(createTerminalGroupRequest);
    }

    @Operation(
            summary = "删除终端组",
            description = "删除指定终端组及其子组，需要组下没有设备",
            tags = {"终端组管理"}
    )
    @Override
    public void deleteTerminalGroup(Long tgid) {
        terminalGroupFacade.deleteTerminalGroup(tgid);
    }

    @Operation(
            summary = "更新终端组",
            description = "更新终端组信息",
            tags = {"终端组管理"}
    )
    @Override
    public void updateTerminalGroup(UpdateTerminalGroupRequest updateTerminalGroupRequest) {
        terminalGroupFacade.updateTerminalGroup(updateTerminalGroupRequest);
    }

    @Operation(
            summary = "获取终端组列表",
            description = "根据条件分页获取终端组列表",
            tags = {"终端组管理"}
    )
    @Override
    public PageVO<TerminalGroupListResponse> listTerminalGroup(PageRequestDTO<QueryTerminalGroupListRequest> requestDTO) {
        return terminalGroupFacade.listTerminalGroup(requestDTO);
    }

    @Operation(
            summary = "搜索终端组",
            description = "根据关键词搜索终端组",
            tags = {"终端组管理"}
    )
    @Override
    public PageVO<TerminalGroupListResponse> searchTerminalGroup(PageRequestDTO<SearchTerminalGroupRequest> requestDTO) {
        return terminalGroupFacade.searchTerminalGroup(requestDTO);
    }

    @Operation(
            summary = "获取终端组详情",
            description = "根据终端组ID获取详细信息",
            tags = {"终端组管理"}
    )
    @Override
    public TerminalGroupDetailResponse getTerminalGroupDetail(Long tgid) {
        return terminalGroupFacade.getTerminalGroupDetail(tgid);
    }

    @Operation(
            summary = "获取子终端组",
            description = "获取指定终端组的直接子级终端组",
            tags = {"终端组管理"}
    )
    @Override
    public List<TerminalGroupDetailResponse> getChildrenTerminalGroups(Long tgid) {
        return terminalGroupFacade.getChildrenTerminalGroups(tgid);
    }

    @Operation(
            summary = "绑定用户组到终端组",
            description = "将用户组绑定到指定终端组",
            tags = {"绑定管理"}
    )
    @Override
    public void bindUserGroupToTerminalGroup(BindUserGroupRequest bindUserGroupRequest) {
        terminalGroupFacade.bindUserGroupToTerminalGroup(bindUserGroupRequest);
    }

    @Operation(
            summary = "解绑用户组",
            description = "从终端组中解绑指定用户组",
            tags = {"绑定管理"}
    )
    @Override
    public void unbindUserGroupFromTerminalGroup(Long tgid, Long ugid) {
        terminalGroupFacade.unbindUserGroupFromTerminalGroup(tgid, ugid);
    }

    @Operation(
            summary = "获取终端组绑定的用户组",
            description = "获取终端组绑定的所有用户组",
            tags = {"绑定管理"}
    )
    @Override
    public PageVO<UserGroupBindingResponse> getTerminalGroupBindings(PageRequestDTO<QueryUserGroupBindingRequest> requestDTO) {
        return terminalGroupFacade.getTerminalGroupBindings(requestDTO);
    }

    @Operation(
            summary = "获取用户可访问的终端组",
            description = "获取当前用户有权限访问的终端组列表",
            tags = {"权限管理"}
    )
    @Override
    public List<TerminalGroupDetailResponse> getAccessibleTerminalGroups() {
        return terminalGroupFacade.getAccessibleTerminalGroups();
    }

    @Operation(
            summary = "检查终端组权限",
            description = "检查当前用户对指定终端组的权限",
            tags = {"权限管理"}
    )
    @Override
    public TerminalGroupPermissionResponse checkTerminalGroupPermission(Long tgid) {
        return terminalGroupFacade.checkTerminalGroupPermission(tgid);
    }

    @Operation(
            summary = "批量权限检查",
            description = "批量检查多个终端组的权限",
            tags = {"权限管理"}
    )
    @Override
    public List<TerminalGroupPermissionResponse> batchCheckPermissions(BatchPermissionCheckRequest batchPermissionCheckRequest) {
        return terminalGroupFacade.batchCheckPermissions(batchPermissionCheckRequest);
    }

    @Operation(
            summary = "获取终端组统计信息",
            description = "获取终端组的统计信息（设备数量、子组数量等）",
            tags = {"统计分析"}
    )
    @Override
    public TerminalGroupStatisticsResponse getTerminalGroupStatistics(Long tgid) {
        return terminalGroupFacade.getTerminalGroupStatistics(tgid);
    }

    @Operation(
            summary = "批量操作终端组",
            description = "批量创建、更新或删除终端组",
            tags = {"批量操作"}
    )
    @Override
    public BatchOperationResponse batchOperateTerminalGroups(BatchTerminalGroupOperationRequest batchOperationRequest) {
        return terminalGroupFacade.batchOperateTerminalGroups(batchOperationRequest);
    }

    @Operation(
            summary = "获取终端组操作历史",
            description = "获取终端组的操作历史记录",
            tags = {"历史记录"}
    )
    @Override
    public PageVO<TerminalGroupHistoryResponse> getTerminalGroupHistory(PageRequestDTO<QueryTerminalGroupHistoryRequest> requestDTO) {
        return terminalGroupFacade.getTerminalGroupHistory(requestDTO);
    }
}