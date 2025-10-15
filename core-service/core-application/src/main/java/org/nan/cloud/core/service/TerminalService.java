package org.nan.cloud.core.service;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.DTO.CreateTerminalDTO;
import org.nan.cloud.core.DTO.QueryTerminalListDTO;
import org.nan.cloud.core.domain.Terminal;

public interface TerminalService {

    void createTerminal(CreateTerminalDTO createTerminalDTO);

    PageVO<Terminal> pageTerminals(int pageNum, int pageSize, QueryTerminalListDTO dto);
}
