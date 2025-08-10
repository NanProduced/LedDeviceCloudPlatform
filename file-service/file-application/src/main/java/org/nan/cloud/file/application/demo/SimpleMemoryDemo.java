package org.nan.cloud.file.application.demo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

/**
 * 简化版内存优化演示
 * 不依赖外部框架，直接演示流式处理的内存优化效果
 */
public class SimpleMemoryDemo {
    
    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();
    
    public static void main(String[] args) {
        try {
            System.out.println("=== VSN文件生成内存优化演示 ===\n");
            
            // 设置较小的堆内存进行测试 (-Xmx512m)
            printMemoryInfo("程序启动");
            
            // 演示1：流式MD5计算 vs 传统MD5计算
            demonstrateStreamingHashCalculation();
            
            System.out.println("\n" + "=".repeat(50) + "\n");
            
            // 演示2：内存使用模式对比
            demonstrateMemoryUsagePatterns();
            
            System.out.println("\n=== 演示结束 ===");
            
        } catch (Exception e) {
            System.err.println("演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demonstrateStreamingHashCalculation() throws Exception {
        System.out.println("【演示1】流式MD5计算 vs 传统MD5计算");
        
        // 创建测试数据 - 生成约30MB的XML数据
        System.out.println("生成测试XML数据...");
        String testData = generateLargeXmlContent(30000);
        int dataSizeMB = testData.getBytes(StandardCharsets.UTF_8).length / 1024 / 1024;
        System.out.println("XML数据大小: " + dataSizeMB + "MB");
        printMemoryInfo("XML数据生成后");
        
        // 方法1：传统方式 - 先转byte[]再计算MD5
        System.out.println("\n--- 传统MD5计算方式 ---");
        long traditionalBefore = getUsedMemoryMB();
        
        byte[] dataBytes = testData.getBytes(StandardCharsets.UTF_8);
        String traditionalMd5 = calculateTraditionalMD5(dataBytes);
        
        long traditionalAfter = getUsedMemoryMB();
        long traditionalMemoryIncrease = traditionalAfter - traditionalBefore;
        
        System.out.println("传统方式结果:");
        System.out.println("  MD5: " + traditionalMd5);
        System.out.println("  内存增长: " + traditionalMemoryIncrease + "MB");
        printMemoryInfo("传统方式计算后");
        
        // 清理内存
        dataBytes = null;
        forceGC();
        
        // 方法2：流式方式 - 使用流计算MD5
        System.out.println("\n--- 流式MD5计算方式 ---");
        long streamingBefore = getUsedMemoryMB();
        
        String streamingMd5 = calculateStreamingMD5(testData);
        
        long streamingAfter = getUsedMemoryMB();
        long streamingMemoryIncrease = streamingAfter - streamingBefore;
        
        System.out.println("流式方式结果:");
        System.out.println("  MD5: " + streamingMd5);
        System.out.println("  内存增长: " + streamingMemoryIncrease + "MB");
        printMemoryInfo("流式方式计算后");
        
        // 验证MD5值一致性
        System.out.println("\n--- 结果对比 ---");
        boolean md5Match = traditionalMd5.equals(streamingMd5);
        System.out.println("MD5值一致: " + (md5Match ? "✓" : "✗"));
        
        if (md5Match && traditionalMemoryIncrease > streamingMemoryIncrease) {
            long memorySaved = traditionalMemoryIncrease - streamingMemoryIncrease;
            double optimizationRatio = (double) memorySaved / traditionalMemoryIncrease * 100;
            System.out.println("内存节省: " + memorySaved + "MB");
            System.out.println("优化比例: " + String.format("%.1f%%", optimizationRatio));
        }
        
        // 清理
        testData = null;
        forceGC();
    }
    
    private static void demonstrateMemoryUsagePatterns() throws Exception {
        System.out.println("【演示2】内存使用模式对比分析");
        
        printMemoryInfo("演示2开始");
        
        // 演示HashingOutputStream功能
        System.out.println("\n--- HashingOutputStream演示 ---");
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "streaming_demo.txt");
        
        long hashingBefore = getUsedMemoryMB();
        
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile());
             HashingOutputStream hashingOut = new HashingOutputStream(fos)) {
            
            // 模拟写入大量数据
            String sampleData = "这是测试数据，会被重复写入文件以模拟大型VSN文件的生成过程。";
            byte[] sampleBytes = sampleData.getBytes(StandardCharsets.UTF_8);
            
            for (int i = 0; i < 50000; i++) {
                hashingOut.write(sampleBytes);
                if (i % 10000 == 0) {
                    hashingOut.flush(); // 定期刷新
                    System.out.println("  已写入: " + (i * sampleBytes.length / 1024) + "KB");
                }
            }
            
            String finalMd5 = hashingOut.getMD5Hex();
            long bytesWritten = hashingOut.getBytesWritten();
            
            System.out.println("HashingOutputStream结果:");
            System.out.println("  写入字节数: " + (bytesWritten / 1024 / 1024) + "MB");
            System.out.println("  文件MD5: " + finalMd5);
            System.out.println("  文件大小: " + (Files.size(tempFile) / 1024 / 1024) + "MB");
        }
        
        long hashingAfter = getUsedMemoryMB();
        long hashingMemoryIncrease = hashingAfter - hashingBefore;
        System.out.println("  内存增长: " + hashingMemoryIncrease + "MB");
        printMemoryInfo("HashingOutputStream演示后");
        
        // 清理临时文件
        Files.deleteIfExists(tempFile);
        
        System.out.println("\n--- VSN生成优化总结 ---");
        System.out.println("原始问题：");
        System.out.println("1. 大XML字符串完整加载到内存");
        System.out.println("2. 字符串转字节数组时占用双倍内存");
        System.out.println("3. MD5计算需要完整字节数组");
        
        System.out.println("\n优化方案：");
        System.out.println("1. 流式XML生成 - 边生成边写入文件");
        System.out.println("2. 流式MD5计算 - 边读取边计算哈希");
        System.out.println("3. 固定缓冲区大小 - 内存使用可控");
        System.out.println("4. 即时内存释放 - 避免内存积压");
        
        System.out.println("\n适用场景：");
        System.out.println("• 大型VSN文件 (>10MB)");
        System.out.println("• 内存受限环境");
        System.out.println("• 高并发场景");
        System.out.println("• 避免OOM风险");
    }
    
    /**
     * 生成大型XML内容用于测试
     */
    private static String generateLargeXmlContent(int itemCount) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Programs>\n");
        xml.append("  <Program>\n");
        xml.append("    <Information>\n");
        xml.append("      <Width>1920</Width>\n");
        xml.append("      <Height>1080</Height>\n");
        xml.append("    </Information>\n");
        xml.append("    <Pages>\n");
        xml.append("      <Page>\n");
        xml.append("        <AppointDuration>10000</AppointDuration>\n");
        xml.append("        <LoopType>1</LoopType>\n");
        xml.append("        <Regions>\n");
        xml.append("          <Region>\n");
        xml.append("            <Rect>\n");
        xml.append("              <X>0</X><Y>0</Y><Width>1920</Width><Height>1080</Height>\n");
        xml.append("            </Rect>\n");
        xml.append("            <Items>\n");
        
        // 生成大量项目
        for (int i = 0; i < itemCount; i++) {
            xml.append("              <Item>\n");
            xml.append("                <Type>TEXT</Type>\n");
            xml.append("                <Text>这是第").append(i).append("个测试项目，包含中文字符、数字和特殊符号!@#$%^&*()。");
            xml.append("用于模拟大型VSN文件的内容，测试流式处理的内存优化效果。</Text>\n");
            xml.append("                <TextColor>0xFF000000</TextColor>\n");
            xml.append("                <Duration>5000</Duration>\n");
            xml.append("                <Alpha>255</Alpha>\n");
            xml.append("              </Item>\n");
        }
        
        xml.append("            </Items>\n");
        xml.append("          </Region>\n");
        xml.append("        </Regions>\n");
        xml.append("      </Page>\n");
        xml.append("    </Pages>\n");
        xml.append("  </Program>\n");
        xml.append("</Programs>");
        
        return xml.toString();
    }
    
    /**
     * 传统方式计算MD5
     */
    private static String calculateTraditionalMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("计算MD5失败", e);
        }
    }
    
    /**
     * 流式计算MD5
     */
    private static String calculateStreamingMD5(String data) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        
        byte[] buffer = new byte[8192]; // 8KB缓冲区
        int bytesRead;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            md5.update(buffer, 0, bytesRead);
        }
        
        return bytesToHex(md5.digest());
    }
    
    /**
     * 简化的HashingOutputStream
     */
    private static class HashingOutputStream extends java.io.OutputStream {
        private final FileOutputStream targetStream;
        private final MessageDigest md5;
        private long bytesWritten = 0;
        
        public HashingOutputStream(FileOutputStream targetStream) throws Exception {
            this.targetStream = targetStream;
            this.md5 = MessageDigest.getInstance("MD5");
        }
        
        @Override
        public void write(int b) throws java.io.IOException {
            targetStream.write(b);
            md5.update((byte) b);
            bytesWritten++;
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            targetStream.write(b, off, len);
            md5.update(b, off, len);
            bytesWritten += len;
        }
        
        @Override
        public void flush() throws java.io.IOException {
            targetStream.flush();
        }
        
        @Override
        public void close() throws java.io.IOException {
            targetStream.close();
        }
        
        public String getMD5Hex() {
            return bytesToHex(md5.digest());
        }
        
        public long getBytesWritten() {
            return bytesWritten;
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private static void printMemoryInfo(String stage) {
        MemoryUsage heapMemory = MEMORY_BEAN.getHeapMemoryUsage();
        long usedMB = heapMemory.getUsed() / 1024 / 1024;
        long maxMB = heapMemory.getMax() / 1024 / 1024;
        double usagePercent = (double) heapMemory.getUsed() / heapMemory.getMax() * 100;
        
        System.out.printf("[%s] 内存: %dMB/%dMB (%.1f%%)%n", stage, usedMB, maxMB, usagePercent);
    }
    
    private static long getUsedMemoryMB() {
        return MEMORY_BEAN.getHeapMemoryUsage().getUsed() / 1024 / 1024;
    }
    
    private static void forceGC() {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}