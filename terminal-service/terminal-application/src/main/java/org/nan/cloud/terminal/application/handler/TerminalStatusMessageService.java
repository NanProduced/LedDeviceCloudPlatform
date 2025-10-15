package org.nan.cloud.terminal.application.handler;

import org.nan.cloud.terminal.application.domain.TerminalStatusReport;

/**
 * 终端上报数据更新推送消息接口
 */
public interface TerminalStatusMessageService {

    void sendTerminalStatusMessage(Long oid, Long tid, TerminalStatusReport report);
}
