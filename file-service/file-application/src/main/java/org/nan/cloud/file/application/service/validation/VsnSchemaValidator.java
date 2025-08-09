package org.nan.cloud.file.application.service.validation;

import org.nan.cloud.program.document.VsnProgram;

import java.util.List;

/**
 * VSN 严格模式校验器（占位接口，后续补全）
 */
public interface VsnSchemaValidator {
    void validate(List<VsnProgram> programs);
}

