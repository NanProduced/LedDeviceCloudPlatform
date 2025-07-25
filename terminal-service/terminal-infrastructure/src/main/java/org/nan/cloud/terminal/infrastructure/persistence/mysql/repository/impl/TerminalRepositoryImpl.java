package org.nan.cloud.terminal.infrastructure.persistence.mysql.repository.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.entity.TerminalInfoDO;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.mapper.TerminalInfoMapper;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.repository.TerminalInfoRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TerminalInfoRepositoryImpl implements TerminalInfoRepository {

    private final TerminalInfoMapper terminalInfoMapper;

    @Override
    public TerminalInfoDO getInfoByTid(Long tid) {
        return terminalInfoMapper.selectById(tid);
    }
}
