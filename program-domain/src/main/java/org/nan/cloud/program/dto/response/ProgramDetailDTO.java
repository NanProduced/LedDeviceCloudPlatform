package org.nan.cloud.program.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.program.document.ProgramPage;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 节目详细信息响应DTO
 * 包含节目的完整内容信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgramDetailDTO {

    private ProgramDTO program;

    private String contentData;

    private String vsnXml;


}