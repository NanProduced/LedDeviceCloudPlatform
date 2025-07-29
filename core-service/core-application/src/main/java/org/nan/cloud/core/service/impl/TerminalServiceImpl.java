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
import org.nan.cloud.core.service.TerminalService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminalServiceImpl implements TerminalService {

    private final TerminalRepository terminalRepository;
    private final TerminalGroupRepository terminalGroupRepository;

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
        Map<Long, String> groupMap = new HashMap<>();
        Set<Long> filterTgids = null;
        if (dto.isIfIncludeSubGroups()) {
            filterTgids = terminalGroupRepository.getAllTgidsByParent(dto.getTgid());
        } else {
            filterTgids = Collections.singleton(dto.getTgid());
        }
        List<Long> filterTids = null;
        if (Objects.nonNull(dto.getOnlineStatus())) {

        }

        
        PageVO<Terminal> terminalPageVO = terminalRepository.pageTerminals(
                pageNum, pageSize, dto.getOid(), groupMap.keySet(), 
                dto.getKeyword(), dto.getTerminalModel(), dto.getOnlineStatus());
        
        // 设置终端组名称
        terminalPageVO.getRecords().forEach(terminal -> {
            String tgName = groupMap.get(terminal.getTgid());
            // 注意：Terminal domain对象没有tgName字段，这里只是示例
            // 实际应该在响应层设置tgName
        });
        
        return terminalPageVO;
    }
}
