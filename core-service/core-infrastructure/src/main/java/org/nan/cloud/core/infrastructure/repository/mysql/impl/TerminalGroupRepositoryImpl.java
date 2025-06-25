package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TerminalGroupDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.TerminalGroupMapper;
import org.nan.cloud.core.repository.TerminalGroupRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class TerminalGroupRepositoryImpl implements TerminalGroupRepository {

    private final TerminalGroupMapper terminalGroupMapper;

    private final CommonConverter commonConverter;

    @Override
    public TerminalGroup createTerminalGroup(TerminalGroup terminalGroup) {
        TerminalGroupDO terminalGroupDO = commonConverter.terminalGroup2TerminalGroupDO(terminalGroup);
        terminalGroupDO.setCreateTime(LocalDateTime.now());
        terminalGroupMapper.insert(terminalGroupDO);
        String path = terminalGroupDO.getPath().isBlank() ? String.valueOf(terminalGroupDO.getTgid()) : terminalGroupDO.getPath() + "|" + terminalGroupDO.getTgid();
        terminalGroupMapper.updateById(terminalGroupDO);
        return commonConverter.terminalGroupDO2TerminalGroup(terminalGroupDO);
    }
}
