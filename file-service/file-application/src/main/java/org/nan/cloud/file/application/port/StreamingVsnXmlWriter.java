package org.nan.cloud.file.application.port;

import org.nan.cloud.program.document.VsnProgram;

import java.io.OutputStream;
import java.util.List;

/**
 * 流式 VSN XML 写出端口
 * 直接写入到输出流，避免大文件在内存中完整存在
 */
public interface StreamingVsnXmlWriter {
    
    /**
     * 将VSN程序列表以流式方式写入输出流
     * 
     * @param programs VSN程序列表
     * @param outputStream 输出流
     * @return 写入的字节数
     * @throws Exception 写入异常
     */
    long writeToStream(List<VsnProgram> programs, OutputStream outputStream) throws Exception;
}