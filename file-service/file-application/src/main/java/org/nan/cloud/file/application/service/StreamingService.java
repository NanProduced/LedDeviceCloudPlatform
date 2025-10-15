package org.nan.cloud.file.application.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.CompletableFuture;

/**
 * 流式处理服务接口
 * 
 * 提供高性能的文件流式传输服务：
 * 1. NIO零拷贝传输 - 减少内存占用和CPU开销
 * 2. 分片流式下载 - 支持大文件断点续传
 * 3. 异步流处理 - 非阻塞式文件传输
 * 4. 内存控制 - 智能缓冲区管理
 * 5. 带宽限制 - 防止网络拥塞
 * 
 * 性能目标：
 * - 内存使用 <100MB（不受文件大小影响）
 * - 传输速度 >50MB/s（本地网络）
 * - CPU使用率 <30%
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface StreamingService {
    
    /**
     * 流式下载文件
     * 使用NIO零拷贝技术，高效传输大文件
     * 
     * @param fileId 文件ID
     * @param rangeStart 范围开始位置（字节，可选）
     * @param rangeEnd 范围结束位置（字节，可选）
     * @return ResponseEntity包含文件资源和响应头
     */
    ResponseEntity<Resource> streamDownload(String fileId, Long rangeStart, Long rangeEnd);
    
    /**
     * 异步流式下载
     * 适用于需要后台处理的大文件下载
     * 
     * @param fileId 文件ID
     * @param rangeStart 范围开始位置
     * @param rangeEnd 范围结束位置
     * @return CompletableFuture包装的ResponseEntity
     */
    CompletableFuture<ResponseEntity<Resource>> streamDownloadAsync(String fileId, Long rangeStart, Long rangeEnd);
    
    /**
     * 获取文件读取通道
     * 用于直接的NIO操作
     * 
     * @param fileId 文件ID
     * @return 可读字节通道
     */
    ReadableByteChannel getFileChannel(String fileId);
    
    /**
     * 获取缓冲的输入流
     * 针对文件大小优化缓冲区大小
     * 
     * @param fileId 文件ID
     * @return 优化后的输入流
     */
    InputStream getBufferedInputStream(String fileId);
    
    /**
     * 流式传输统计
     * 获取传输性能统计信息
     * 
     * @return 传输统计
     */
    StreamingStatistics getStreamingStatistics();
    
    /**
     * 预加载文件到内存（小文件）
     * 将小文件预加载到内存缓存中，提升访问速度
     * 
     * @param fileId 文件ID
     * @param maxSize 最大预加载大小（字节）
     * @return 是否成功预加载
     */
    boolean preloadFile(String fileId, long maxSize);
    
    /**
     * 获取传输进度
     * 用于长时间传输的进度跟踪
     * 
     * @param transferId 传输ID
     * @return 传输进度（0.0 - 1.0）
     */
    double getTransferProgress(String transferId);
    
    /**
     * 取消传输
     * 取消正在进行的文件传输
     * 
     * @param transferId 传输ID
     * @return 是否成功取消
     */
    boolean cancelTransfer(String transferId);
    
    /**
     * 流式传输统计信息
     */
    interface StreamingStatistics {
        long getTotalTransferredBytes();
        long getActiveTransferCount();
        double getAverageThroughput();
        long getPeakThroughput();
        double getAverageResponseTime();
        long getCacheHitCount();
        long getCacheMissCount();
        double getCacheHitRate();
    }
}