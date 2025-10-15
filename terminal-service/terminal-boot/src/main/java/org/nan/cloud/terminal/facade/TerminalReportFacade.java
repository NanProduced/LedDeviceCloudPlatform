package org.nan.cloud.terminal.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.terminal.application.service.TerminalReportService;
import org.nan.cloud.terminal.config.security.auth.TerminalPrincipal;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TerminalReportFacade {

    private final TerminalReportService terminalReportService;

    public void handlerTerminalStatusReport(TerminalPrincipal terminal, String report) {
        terminalReportService.tryToSaveTerminalReport(terminal.getOid(), terminal.getTid(), terminal.getTerminalName(), report);
    }
}
