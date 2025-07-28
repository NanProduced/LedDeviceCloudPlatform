package org.nan.cloud.terminal.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.exception.Terminal400Exception;
import org.nan.cloud.common.basic.utils.StringUtils;
import org.nan.cloud.common.web.IgnoreDynamicResponse;
import org.nan.cloud.terminal.api.TerminalApi;
import org.nan.cloud.terminal.api.common.model.TerminalCommand;
import org.nan.cloud.terminal.cache.TerminalOnlineStatusManager;
import org.nan.cloud.terminal.config.security.auth.TerminalPrincipal;
import org.nan.cloud.terminal.facade.TerminalReportFacade;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Terminal Http request", description = "Terminal API")
@RestController
@RequiredArgsConstructor
public class TerminalController implements TerminalApi {

    private final TerminalReportFacade terminalReportFacade;

    private final TerminalOnlineStatusManager terminalOnlineStatusManager;

    @Override
    @IgnoreDynamicResponse
    public void reportTerminalStatus(String report) {
        TerminalPrincipal principal = (TerminalPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        terminalOnlineStatusManager.updateTerminalActivity(principal.getOid(), principal.getTid());
        if (StringUtils.isNotBlank(report)) {
            terminalReportFacade.handlerTerminalStatusReport(principal, report);
        }
        else throw new Terminal400Exception();
    }

    @Override
    @IgnoreDynamicResponse
    public List<TerminalCommand> getCommands(String clt_type, Integer device_num) {
        return List.of();
    }
}
