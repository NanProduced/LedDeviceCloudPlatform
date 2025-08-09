package org.nan.cloud.file.application.port;

import org.nan.cloud.program.document.VsnProgram;

import java.util.List;

/**
 * VSN XML 写出端口（在 infrastructure 实现）
 */
public interface VsnXmlWriter {
    String write(List<VsnProgram> programs);
}

