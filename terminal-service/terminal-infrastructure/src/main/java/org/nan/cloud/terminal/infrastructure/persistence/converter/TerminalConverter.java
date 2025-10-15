package org.nan.cloud.terminal.infrastructure.persistence.converter;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.terminal.application.domain.TerminalAccount;
import org.nan.cloud.terminal.application.domain.TerminalInfo;
import org.nan.cloud.terminal.infrastructure.entity.auth.TerminalAccountDO;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.entity.TerminalInfoDO;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface TerminalConverter {

    TerminalAccount convert2TerminalAccount(TerminalAccountDO terminalAccountDO);

    TerminalAccountDO convert2TerminalAccountDO(TerminalAccount terminalAccount);

    TerminalInfo convert2TerminalInfo(TerminalInfoDO terminalInfoDO);

    TerminalInfoDO convert2TerminalInfoDO(TerminalInfo terminalInfo);
}
