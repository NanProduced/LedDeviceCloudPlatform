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
            summary = "获取终端组详情",
            description = "根据终端组ID获取详细信息",
            tags = {"终端组管理"}
    )
    @Override
    public TerminalGroupDetailResponse getTerminalGroupDetail(Long tgid) {
        return terminalGroupFacade.getTerminalGroupDetail(tgid);
    }


    @Operation(
            summary = "更新权限表达式",
            description = "全量替换用户组的权限绑定，支持INCLUDE/EXCLUDE权限类型和智能冗余清理",
            tags = {"权限管理"}
    )
    @Override
    public PermissionExpressionResponse updatePermissionExpression(PermissionExpressionRequest request) {
        return terminalGroupFacade.updatePermissionExpression(request);
    }

    @Operation(
            summary = "获取用户组权限状态",
            description = "获取用户组当前的权限绑定状态和统计信息，用于前端权限树展示",
            tags = {"权限管理"}
    )
    @Override
    public UserGroupPermissionStatusResponse getUserGroupPermissionStatus(Long ugid) {
        return terminalGroupFacade.getUserGroupPermissionStatus(ugid);
    }

}