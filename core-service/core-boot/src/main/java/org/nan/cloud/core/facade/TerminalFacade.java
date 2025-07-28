package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.DTO.CreateTerminalDTO;
import org.nan.cloud.core.DTO.QueryTerminalListDTO;
import org.nan.cloud.core.api.DTO.req.CreateTerminalRequest;
import org.nan.cloud.core.api.DTO.req.QueryTerminalListRequest;
import org.nan.cloud.core.api.DTO.res.TerminalListResponse;
import org.nan.cloud.core.aspect.SkipOrgManagerPermissionCheck;
import org.nan.cloud.core.domain.Terminal;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.service.PermissionChecker;
import org.nan.cloud.core.service.TerminalGroupService;
import org.nan.cloud.core.service.TerminalService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TerminalFacade {

    private final PermissionChecker permissionChecker;

    private final TerminalService terminalService;
    
    private final TerminalGroupService terminalGroupService;

    @Transactional(rollbackFor = Exception.class)
    @SkipOrgManagerPermissionCheck
    public void createTerminal(CreateTerminalRequest createTerminalRequest) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.TERMINAL_GROUP_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetTerminalGroup(requestUser.getOid(), requestUser.getUgid(), createTerminalRequest.getTgid()));
        CreateTerminalDTO createTerminalDTO = CreateTerminalDTO.builder()
                .terminalName(createTerminalRequest.getTerminalName())
                .description(createTerminalRequest.getDescription())
                .terminalAccount(createTerminalRequest.getTerminalAccount())
                .terminalPassword(createTerminalRequest.getTerminalPassword())
                .tgid(createTerminalRequest.getTgid())
                .oid(requestUser.getOid())
                .createdBy(requestUser.getUid())
                .build();
        terminalService.createTerminal(createTerminalDTO);
    }

    public PageVO<TerminalListResponse> listTerminals(PageRequestDTO<QueryTerminalListRequest> requestDTO) {
        Long ugid = InvocationContextHolder.getUgid();
        Long oid = InvocationContextHolder.getOid();
        
        // 权限校验：检查用户是否有终端组权限
        ExceptionEnum.TERMINAL_GROUP_PERMISSION_DENIED.throwIf(
                !permissionChecker.ifHasPermissionOnTargetTerminalGroup(ugid, requestDTO.getParams().getTgid()));
        
        QueryTerminalListDTO dto = QueryTerminalListDTO.builder()
                .oid(oid)
                .tgid(requestDTO.getParams().getTgid())
                .ifIncludeSubGroups(requestDTO.getParams().isIncludeSubGroups())
                .keyword(requestDTO.getParams().getKeyword())
                .terminalModel(requestDTO.getParams().getTerminalModel())
                .onlineStatus(requestDTO.getParams().getOnlineStatus())
                .build();
        
        PageVO<Terminal> terminalPageVO = terminalService.pageTerminals(
                requestDTO.getPageNum(), requestDTO.getPageSize(), dto);
        
        if (CollectionUtils.isEmpty(terminalPageVO.getRecords())) {
            return PageVO.empty();
        }
        
        // 获取终端组信息，用于设置终端组名称
        Map<Long, String> terminalGroupMap = terminalPageVO.getRecords().stream()
                .map(Terminal::getTgid)
                .distinct()
                .collect(Collectors.toMap(
                        tgid -> tgid,
                        tgid -> {
                            TerminalGroup group = terminalGroupService.getTerminalGroupById(tgid);
                            return group != null ? group.getName() : "未知分组";
                        }
                ));
        
        return terminalPageVO.map(terminal -> TerminalListResponse.builder()
                .tid(terminal.getTid())
                .terminalName(terminal.getTerminalName())
                .description(terminal.getDescription())
                .terminalModel(terminal.getTerminalModel())
                .tgid(terminal.getTgid())
                .tgName(terminalGroupMap.get(terminal.getTgid()))
                .firmwareVersion(terminal.getFirmwareVersion())
                .serialNumber(terminal.getSerialNumber())
                .onlineStatus(null) // TODO: 实现终端在线状态查询
                .createdAt(terminal.getCreatedAt())
                .updatedAt(terminal.getUpdatedAt())
                .createdBy(terminal.getCreatedBy())
                .build());
    }
}
