package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.basic.utils.PasswordUtils;
import org.nan.cloud.core.DTO.CreateTerminalDTO;
import org.nan.cloud.core.DTO.QueryTerminalListDTO;
import org.nan.cloud.core.domain.Terminal;
import org.nan.cloud.core.domain.TerminalAccount;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.repository.TerminalGroupRepository;
import org.nan.cloud.core.repository.TerminalRepository;
import org.nan.cloud.core.service.TerminalCacheService;
import org.nan.cloud.core.service.TerminalService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminalServiceImpl implements TerminalService {

    private final TerminalRepository terminalRepository;
    private final TerminalGroupRepository terminalGroupRepository;
    private final TerminalCacheService terminalCacheService;

    @Override
    public void createTerminal(CreateTerminalDTO createTerminalDTO) {
        TerminalAccount  terminalAccount = TerminalAccount.builder()
                .account(createTerminalDTO.getTerminalAccount())
                .password(PasswordUtils.encodeByBCrypt(createTerminalDTO.getTerminalPassword()))
                .oid(createTerminalDTO.getOid())
                .createBy(createTerminalDTO.getCreatedBy())
                .build();
        Long tid = terminalRepository.createTerminalAccount(terminalAccount);
        ExceptionEnum.CREATE_TERMINAL_ACCOUNT_FAILED.throwIf(tid == null);
        // todo: 终端重名检测/终端账号名重名检测
        Terminal terminal = Terminal.builder()
                .tid(tid)
                .terminalName(createTerminalDTO.getTerminalName())
                .description(createTerminalDTO.getDescription())
                .oid(createTerminalDTO.getOid())
                .tgid(createTerminalDTO.getTgid())
                .createdBy(createTerminalDTO.getCreatedBy())
                .build();
        terminalRepository.createTerminal(terminal);
    }

    @Override
    public PageVO<Terminal> pageTerminals(int pageNum, int pageSize, QueryTerminalListDTO dto) {
        Set<Long> filterTgids;
        if (dto.isIfIncludeSubGroups()) {
            filterTgids = terminalGroupRepository.getAllTgidsByParent(dto.getTgid());
        } else {
            filterTgids = Collections.singleton(dto.getTgid());
        }
        Set<Long> filterTids = null;
        Set<Long> onlineTids = terminalCacheService.getOnlineTidsByOid(dto.getOid());
        if (Objects.nonNull(dto.getOnlineStatus())) {
            filterTids = onlineTids;
            // 无在线终端直接返回
            if (dto.getOnlineStatus().equals(1) && CollectionUtils.isEmpty(filterTids)) {
                return PageVO.empty();
            }
        }
        PageVO<Terminal> terminalPageVO = terminalRepository.pageTerminals(
                pageNum, pageSize, dto.getOid(), filterTgids,
                dto.getKeyword(), dto.getTerminalModel(), dto.getOnlineStatus(), filterTids);
        // 终端在线状态设置
        terminalPageVO.getRecords().forEach(e -> e.setOnlineStatus(onlineTids.contains(e.getTid()) ? 1 : 0));
        return terminalPageVO;
    }
}
