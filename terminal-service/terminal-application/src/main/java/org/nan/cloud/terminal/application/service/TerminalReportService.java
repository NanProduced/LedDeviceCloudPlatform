package org.nan.cloud.terminal.application.service;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.terminal.application.domain.TerminalStatusReport;
import org.nan.cloud.terminal.application.handler.TerminalStatusMessageService;
import org.nan.cloud.terminal.application.repository.TerminalReportRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TerminalReportService {

    private final TerminalReportRepository terminalReportRepository;

    private final TerminalStatusMessageService  terminalStatusMessageService;

    public void tryToSaveTerminalReport(Long oid, Long tid, String terminalName, String report) {
        try {
            TerminalStatusReport terminalStatusReport = JsonUtils.fromJson(report, TerminalStatusReport.class);
            handlerTerminalStatusMessage(oid, tid, terminalStatusReport);
            terminalReportRepository.updateTerminalStatusReport(oid, tid, terminalName, terminalStatusReport);
        } catch (BaseException e) {
            // ignore
            // 反序列化失败说明不是这类上报（设备遗留问题，多类上报混杂在一个api中）
        }
    }

    /**
     * 异步
     * @param oid
     * @param tid
     * @param terminalStatusReport
     */
    private void handlerTerminalStatusMessage(Long oid, Long tid, TerminalStatusReport terminalStatusReport) {
        terminalStatusMessageService.sendTerminalStatusMessage(oid, tid, terminalStatusReport);
    }
}
