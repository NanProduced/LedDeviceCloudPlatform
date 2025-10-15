package org.nan.cloud.core.infrastructure.repository.mysql.converter;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.domain.Terminal;
import org.nan.cloud.core.domain.TerminalAccount;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TerminalAccountDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TerminalInfoDO;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface TerminalConverter {

    TerminalAccount toTerminalAccount(TerminalAccountDO terminalAccountDO);
    TerminalAccountDO toTerminalAccountDO(TerminalAccount terminalAccount);
    Terminal toTerminal(TerminalInfoDO terminalInfoDO);
    List<Terminal> toTerminalList(List<TerminalInfoDO> terminalInfoDOS);
    TerminalInfoDO toTerminalInfoDO(Terminal terminal);
}
