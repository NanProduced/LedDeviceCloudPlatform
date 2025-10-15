package org.nan.cloud.terminal.application.repository;

import org.nan.cloud.terminal.application.domain.TerminalStatusReport;

public interface TerminalReportRepository {

    TerminalStatusReport getTerminalStatusReportByTid(Long oid, Long tid);

    void updateTerminalStatusReport(Long oid, Long tid, String terminalName, TerminalStatusReport terminalStatusReport);

    void asyncUpsertTerminalStatusReport(Long oid, Long tid, String terminalName, TerminalStatusReport terminalStatusReport);

}
