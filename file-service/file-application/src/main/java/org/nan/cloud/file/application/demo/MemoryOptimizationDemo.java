package org.nan.cloud.file.application.demo;

import org.nan.cloud.file.application.utils.MemoryMonitor;
import org.nan.cloud.file.application.utils.StreamingHashCalculator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 内存优化演示类
 * 演示流式处理 vs 传统处理的内存使用差异
 */
public class MemoryOptimizationDemo {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== VSN文件生成内存优化演示 ===\n");
            
            // 演示1：内存监控功能
            demonstrateMemoryMonitoring();
            
            System.out.println("\n" + "=".repeat(50) + "\n");
            
            // 演示2：流式MD5计算 vs 传统MD5计算
            demonstrateStreamingHashCalculation();
            
            System.out.println("\n" + "=".repeat(50) + "\n");
            
            // 演示3：内存使用对比分析
            demonstrateMemoryUsageComparison();
            
            System.out.println("\n=== 演示结束 ===");
            
        } catch (Exception e) {
            System.err.println("演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demonstrateMemoryMonitoring() {
        System.out.println("【演示1】内存监控功能演示");
        
        MemoryMonitor.MemoryWatcher watcher = new MemoryMonitor.MemoryWatcher("内存监控测试");
        
        // 创建一些测试数据
        System.out.println("1. 创建测试数据...");
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            largeData.append("这是第").append(i).append("行测试数据，包含中文字符和数字。");
        }
        watcher.checkpoint("创建大量字符串数据");
        
        // 转换为字节数组
        System.out.println("2. 转换为字节数组...");
        byte[] dataBytes = largeData.toString().getBytes(StandardCharsets.UTF_8);
        watcher.checkpoint("字符串转字节数组");
        
        System.out.println("3. 数据统计:");
        System.out.println("   字符串长度: " + largeData.length() + " 字符");
        System.out.println("   字节数组大小: " + (dataBytes.length / 1024 / 1024) + " MB");
        
        // 清理数据
        System.out.println("4. 清理数据...");
        largeData = null;
        dataBytes = null;
        watcher.checkpoint("清理测试数据");
        
        watcher.finish();
        
        // 执行垃圾回收
        MemoryMonitor.forceGCAndLog("演示1结束");
    }
    
    private static void demonstrateStreamingHashCalculation() throws Exception {
        System.out.println("【演示2】流式MD5计算 vs 传统MD5计算");
        
        // 创建测试数据
        String testData = generateLargeXmlContent(50000); // 生成约50MB的XML数据
        System.out.println("生成测试XML数据，大小: " + (testData.getBytes(StandardCharsets.UTF_8).length / 1024 / 1024) + "MB");
        
        // 方法1：传统方式 - 先转byte[]再计算MD5
        System.out.println("\n--- 传统MD5计算方式 ---");
        MemoryMonitor.MemorySnapshot traditionalBefore = MemoryMonitor.takeSnapshot();
        
        byte[] dataBytes = testData.getBytes(StandardCharsets.UTF_8);
        String traditionalMd5 = calculateTraditionalMD5(dataBytes);
        
        MemoryMonitor.MemorySnapshot traditionalAfter = MemoryMonitor.takeSnapshot();
        long traditionalMemoryIncrease = traditionalAfter.heapUsed - traditionalBefore.heapUsed;
        
        System.out.println("传统方式结果:");
        System.out.println("  MD5: " + traditionalMd5);
        System.out.println("  内存增长: " + (traditionalMemoryIncrease / 1024 / 1024) + "MB");
        
        // 清理内存
        dataBytes = null;
        System.gc();
        Thread.sleep(100);
        
        // 方法2：流式方式 - 使用流计算MD5
        System.out.println("\n--- 流式MD5计算方式 ---");
        MemoryMonitor.MemorySnapshot streamingBefore = MemoryMonitor.takeSnapshot();
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        String streamingMd5 = StreamingHashCalculator.calculateMD5Hex(inputStream);
        
        MemoryMonitor.MemorySnapshot streamingAfter = MemoryMonitor.takeSnapshot();
        long streamingMemoryIncrease = streamingAfter.heapUsed - streamingBefore.heapUsed;
        
        System.out.println("流式方式结果:");
        System.out.println("  MD5: " + streamingMd5);
        System.out.println("  内存增长: " + (streamingMemoryIncrease / 1024 / 1024) + "MB");
        
        // 验证MD5值一致性
        System.out.println("\n--- MD5一致性验证 ---");
        boolean md5Match = traditionalMd5.equals(streamingMd5);
        System.out.println("MD5值一致: " + (md5Match ? "✓" : "✗"));
        
        if (md5Match) {
            long memorySaved = traditionalMemoryIncrease - streamingMemoryIncrease;
            double optimizationRatio = memorySaved > 0 ? (double) memorySaved / traditionalMemoryIncrease * 100 : 0;
            System.out.println("内存节省: " + (memorySaved / 1024 / 1024) + "MB");
            System.out.println("优化比例: " + String.format("%.1f%%", optimizationRatio));
        }
        
        // 清理
        testData = null;
        MemoryMonitor.forceGCAndLog("演示2结束");
    }
    
    private static void demonstrateMemoryUsageComparison() throws Exception {
        System.out.println("【演示3】内存使用模式对比分析");
        
        // 场景：处理100MB级别的数据
        int dataSize = 100 * 1024 * 1024; // 100MB
        System.out.println("模拟处理 " + (dataSize / 1024 / 1024) + "MB 数据的内存使用情况");
        
        System.out.println("\n--- 传统方式内存使用模式 ---");
        System.out.println("1. XML字符串(UTF-16): ~" + (dataSize * 2 / 1024 / 1024) + "MB");
        System.out.println("2. 字节数组(UTF-8): ~" + (dataSize / 1024 / 1024) + "MB");
        System.out.println("3. MD5计算临时对象: ~" + (dataSize / 1024 / 1024) + "MB");
        System.out.println("峰值内存使用: ~" + ((dataSize * 4) / 1024 / 1024) + "MB");
        
        System.out.println("\n--- 流式处理内存使用模式 ---");
        System.out.println("1. 缓冲区(16KB): ~0.02MB");
        System.out.println("2. MD5状态对象: ~0.01MB");
        System.out.println("3. 输出流缓冲区: ~0.02MB");
        System.out.println("峰值内存使用: ~0.05MB");
        
        System.out.println("\n--- 优化效果总结 ---");
        int traditionalMemory = dataSize * 4;
        int streamingMemory = 64 * 1024; // 64KB
        int memorySaved = traditionalMemory - streamingMemory;
        double optimizationRatio = (double) memorySaved / traditionalMemory * 100;
        
        System.out.println("内存节省: " + (memorySaved / 1024 / 1024) + "MB");
        System.out.println("优化比例: " + String.format("%.2f%%", optimizationRatio));
        System.out.println("适用场景: 大型VSN文件(>10MB)生成时显著降低OOM风险");
        
        // 演示HashingOutputStream的使用
        System.out.println("\n--- HashingOutputStream演示 ---");
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "streaming_demo.txt");
        
        MemoryMonitor.MemorySnapshot hashingBefore = MemoryMonitor.takeSnapshot();
        
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile());
             StreamingHashCalculator.HashingOutputStream hashingOut = 
                     new StreamingHashCalculator.HashingOutputStream(fos)) {
            
            // 模拟写入大量数据
            String sampleData = "这是测试数据，会被重复写入文件。";
            byte[] sampleBytes = sampleData.getBytes(StandardCharsets.UTF_8);
            
            for (int i = 0; i < 10000; i++) {
                hashingOut.write(sampleBytes);
                if (i % 1000 == 0) {
                    hashingOut.flush(); // 定期刷新
                }
            }
            
            String finalMd5 = hashingOut.getMD5Hex();
            long bytesWritten = hashingOut.getBytesWritten();
            
            System.out.println("HashingOutputStream结果:");
            System.out.println("  写入字节数: " + (bytesWritten / 1024) + "KB");
            System.out.println("  文件MD5: " + finalMd5);
            System.out.println("  文件大小: " + (Files.size(tempFile) / 1024) + "KB");
        }
        
        MemoryMonitor.MemorySnapshot hashingAfter = MemoryMonitor.takeSnapshot();
        long hashingMemoryIncrease = hashingAfter.heapUsed - hashingBefore.heapUsed;
        System.out.println("  内存增长: " + (hashingMemoryIncrease / 1024 / 1024) + "MB");
        
        // 清理临时文件
        Files.deleteIfExists(tempFile);
        
        System.out.println("\n关键优化点:");
        System.out.println("1. 避免大字符串在内存中完整存在");
        System.out.println("2. 使用固定大小缓冲区进行流式处理");
        System.out.println("3. 边写入边计算MD5，避免二次读取");
        System.out.println("4. 及时释放不再需要的内存对象");
        System.out.println("5. 使用NIO提高文件I/O效率");
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
            xml.append("                <Text>这是第").append(i).append("个测试项目的内容，包含中文字符、数字和特殊符号!@#$%^&*()。");
            xml.append("用于模拟大型VSN文件的内容，测试流式处理的内存优化效果。");
            xml.append("每个项目都包含相当数量的文本内容，以便生成足够大的XML文件来验证内存使用情况。</Text>\n");
            xml.append("                <TextColor>0xFF000000</TextColor>\n");
            xml.append("                <Duration>5000</Duration>\n");
            xml.append("                <Alpha>255</Alpha>\n");
            xml.append("                <LogFont>\n");
            xml.append("                  <lfHeight>24</lfHeight>\n");
            xml.append("                  <lfWeight>400</lfWeight>\n");
            xml.append("                  <lfFaceName>Microsoft YaHei</lfFaceName>\n");
            xml.append("                </LogFont>\n");
            xml.append("                <FileSource>\n");
            xml.append("                  <IsRelative>1</IsRelative>\n");
            xml.append("                  <FilePath>/test/path/file_").append(i).append(".txt</FilePath>\n");
            xml.append("                  <MD5>abcdef1234567890abcdef1234567890</MD5>\n");
            xml.append("                  <OriginName>测试文件_").append(i).append(".txt</OriginName>\n");
            xml.append("                </FileSource>\n");
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
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算MD5失败", e);
        }
    }
}