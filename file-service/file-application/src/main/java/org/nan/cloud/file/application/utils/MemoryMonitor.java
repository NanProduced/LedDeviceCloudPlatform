package org.nan.cloud.file.application.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;

/**
 * 内存使用监控工具
 * 用于监控VSN文件生成过程中的内存使用情况
 */
@Slf4j
public class MemoryMonitor {
    
    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");
    
    /**
     * 内存使用快照
     */
    public static class MemorySnapshot {
        public final long heapUsed;
        public final long heapMax;
        public final long nonHeapUsed;
        public final long nonHeapMax;
        public final double heapUsagePercent;
        public final long timestamp;
        
        private MemorySnapshot(MemoryUsage heapMemory, MemoryUsage nonHeapMemory) {
            this.timestamp = System.currentTimeMillis();
            this.heapUsed = heapMemory.getUsed();
            this.heapMax = heapMemory.getMax();
            this.nonHeapUsed = nonHeapMemory.getUsed();
            this.nonHeapMax = nonHeapMemory.getMax();
            this.heapUsagePercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
        }
        
        public long getHeapUsedMB() {
            return heapUsed / 1024 / 1024;
        }
        
        public long getHeapMaxMB() {
            return heapMax / 1024 / 1024;
        }
        
        public double getHeapUsagePercent() {
            return heapUsagePercent;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("堆内存: %dMB/%dMB (%.2f%%), 非堆内存: %dMB",
                    getHeapUsedMB(), getHeapMaxMB(), heapUsagePercent, nonHeapUsed / 1024 / 1024);
        }
    }
    
    /**
     * 获取当前内存使用快照
     */
    public static MemorySnapshot takeSnapshot() {
        MemoryUsage heapMemory = MEMORY_BEAN.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = MEMORY_BEAN.getNonHeapMemoryUsage();
        return new MemorySnapshot(heapMemory, nonHeapMemory);
    }
    
    /**
     * 记录内存使用情况
     */
    public static void logMemoryUsage(String operation) {
        MemorySnapshot snapshot = takeSnapshot();
        log.info("[内存监控] {} - {}", operation, snapshot);
        
        // 内存使用率过高时发出警告
        if (snapshot.getHeapUsagePercent() > 80) {
            log.warn("[内存警告] 堆内存使用率过高: {}%, 建议执行GC或优化内存使用", 
                    String.format("%.2f", snapshot.getHeapUsagePercent()));
        }
    }
    
    /**
     * 比较两个内存快照的差异
     */
    public static void logMemoryDifference(String operation, MemorySnapshot before, MemorySnapshot after) {
        long heapDiff = after.heapUsed - before.heapUsed;
        long timeDiff = after.timestamp - before.timestamp;
        
        log.info("[内存监控] {} 耗时: {}ms, 内存变化: {}MB, 当前使用: {}MB ({}%)",
                operation, timeDiff,
                FORMAT.format(heapDiff / 1024.0 / 1024.0),
                after.getHeapUsedMB(),
                FORMAT.format(after.getHeapUsagePercent()));
        
        if (heapDiff > 50 * 1024 * 1024) { // 超过50MB
            log.warn("[内存警告] {} 操作消耗大量内存: {}MB", operation, heapDiff / 1024 / 1024);
        }
    }
    
    /**
     * 执行垃圾回收并记录效果
     */
    public static void forceGCAndLog(String reason) {
        MemorySnapshot before = takeSnapshot();
        log.info("[GC] 执行垃圾回收，原因: {}", reason);
        
        System.gc();
        
        // 等待一小段时间让GC完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        MemorySnapshot after = takeSnapshot();
        long recovered = before.heapUsed - after.heapUsed;
        
        log.info("[GC] 垃圾回收完成，回收内存: {}MB, 当前使用: {}MB ({}%)",
                recovered / 1024 / 1024,
                after.getHeapUsedMB(),
                FORMAT.format(after.getHeapUsagePercent()));
    }
    
    /**
     * 检查内存是否充足
     * 
     * @param requiredMB 预计需要的内存（MB）
     * @return 是否有足够内存
     */
    public static boolean hasEnoughMemory(int requiredMB) {
        MemorySnapshot snapshot = takeSnapshot();
        long availableMB = snapshot.getHeapMaxMB() - snapshot.getHeapUsedMB();
        
        boolean enough = availableMB >= requiredMB;
        
        if (!enough) {
            log.warn("[内存检查] 可用内存不足: 需要{}MB, 可用{}MB", requiredMB, availableMB);
        }
        
        return enough;
    }
    
    /**
     * 内存使用监控器 - 用于长时间运行的操作
     */
    public static class MemoryWatcher {
        private final String operationName;
        private final MemorySnapshot startSnapshot;
        private final long startTime;
        
        public MemoryWatcher(String operationName) {
            this.operationName = operationName;
            this.startSnapshot = takeSnapshot();
            this.startTime = System.currentTimeMillis();
            log.debug("[内存监控] 开始监控操作: {}", operationName);
        }
        
        public void checkpoint(String phase) {
            MemorySnapshot current = takeSnapshot();
            long elapsed = System.currentTimeMillis() - startTime;
            long memoryIncrease = current.heapUsed - startSnapshot.heapUsed;
            
            log.debug("[内存监控] {} - {} 阶段: 耗时{}ms, 内存增长{}MB, 当前{}MB",
                    operationName, phase, elapsed,
                    memoryIncrease / 1024 / 1024,
                    current.getHeapUsedMB());
        }
        
        public void finish() {
            MemorySnapshot endSnapshot = takeSnapshot();
            logMemoryDifference(operationName, startSnapshot, endSnapshot);
        }
    }
}