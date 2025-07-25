package org.nan.cloud.terminal.infrastructure.persistence.mysql.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.terminal.application.domain.TerminalAccount;
import org.nan.cloud.terminal.application.domain.TerminalInfo;
import org.nan.cloud.terminal.infrastructure.entity.auth.TerminalAccountDO;
import org.nan.cloud.terminal.infrastructure.mapper.auth.TerminalAccountMapper;
import org.nan.cloud.terminal.infrastructure.persistence.converter.TerminalConverter;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.entity.TerminalInfoDO;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.mapper.TerminalInfoMapper;
import org.nan.cloud.terminal.application.repository.TerminalRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class TerminalRepositoryImpl implements TerminalRepository {

    private final TerminalInfoMapper terminalInfoMapper;

    private final TerminalAccountMapper terminalAccountMapper;

    private final TerminalConverter terminalConverter;

    @Override
    public TerminalInfo getInfoByTid(Long tid) {
        return terminalConverter.convert2TerminalInfo(terminalInfoMapper.selectById(tid));
    }

    @Override
    public TerminalAccount getAccountByName(String accountName) {
        TerminalAccountDO terminalAccountDO = terminalAccountMapper.selectOne(
                new LambdaQueryWrapper<TerminalAccountDO>()
                        .eq(TerminalAccountDO::getAccount, accountName)
                        .eq(TerminalAccountDO::getDeleted, false)
        );
        return terminalConverter.convert2TerminalAccount(terminalAccountDO);
    }

    @Override
    public void updateLastLogin(Long tid, String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        terminalAccountMapper.updateLastLogin(tid, now, clientIp, now);
    }
}
