package org.nan.cloud.file.infrastructure.vsn;

import org.nan.cloud.program.document.VsnProgram;

import java.util.List;

/**
 * VSN XML 写出器（占位接口，后续补全）
 */
public interface VsnXmlWriter {
    String write(List<VsnProgram> programs);
}

