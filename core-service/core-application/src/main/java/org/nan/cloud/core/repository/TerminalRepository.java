package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.Terminal;
import org.nan.cloud.core.domain.TerminalAccount;

import java.util.Set;

public interface TerminalRepository {

    Long createTerminalAccount(TerminalAccount terminalAccount);

    void createTerminal(Terminal terminal);

    PageVO<Terminal> pageTerminals(int pageNum, int pageSize, Long oid, Set<Long> tgids, String keyword, String terminalModel, Integer onlineStatus);
}
