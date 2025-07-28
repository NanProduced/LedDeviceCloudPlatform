package org.nan.cloud.terminal.infrastructure.persistence.mongodb.document;

import lombok.Data;
import org.nan.cloud.terminal.application.domain.TerminalStatusReport;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document("terminal_status_report")
public class TerminalStatusReportDocument {

    @Id
    private String objectId;

    private Long oid;

    private Long tid;

    private String terminalName;

    private TerminalStatusReport terminalStatusReport;

    private LocalDateTime updateTime;


}
