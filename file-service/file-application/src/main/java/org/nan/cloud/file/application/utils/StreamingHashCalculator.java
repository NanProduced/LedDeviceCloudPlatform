package org.nan.cloud.file.application.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 流式哈希计算工具
 * 支持边读取边计算MD5，避免大文件完全加载到内存
 */
@Slf4j
public class StreamingHashCalculator {
    
    private static final int BUFFER_SIZE = 8192; // 8KB缓冲区
    
    /**
     * 计算输入流的MD5哈希值
     * 
     * @param inputStream 输入流
     * @return MD5十六进制字符串
     * @throws Exception 计算异常
     */
    public static String calculateMD5Hex(InputStream inputStream) throws Exception {
        MessageDigest md5 = createMD5Digest();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, bytesRead);
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.warn("关闭输入流失败: {}", e.getMessage());
            }
        }
        
        return bytesToHex(md5.digest());
    }
    
    /**
     * 计算文件的MD5哈希值
     * 
     * @param filePath 文件路径
     * @return MD5十六进制字符串
     * @throws Exception 计算异常
     */
    public static String calculateFileMD5Hex(String filePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {
            return calculateMD5Hex(bis);
        }
    }
    
    /**
     * 双重哈希输出流 - 同时写入文件和计算MD5
     * 避免文件写入后再次读取计算哈希
     */
    public static class HashingOutputStream extends OutputStream {
        private final OutputStream targetStream;
        private final MessageDigest md5;
        private long bytesWritten = 0;
        
        public HashingOutputStream(OutputStream targetStream) throws NoSuchAlgorithmException {
            this.targetStream = targetStream;
            this.md5 = createMD5Digest();
        }
        
        @Override
        public void write(int b) throws IOException {
            targetStream.write(b);
            md5.update((byte) b);
            bytesWritten++;
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            targetStream.write(b, off, len);
            md5.update(b, off, len);
            bytesWritten += len;
        }
        
        @Override
        public void flush() throws IOException {
            targetStream.flush();
        }
        
        @Override
        public void close() throws IOException {
            targetStream.close();
        }
        
        /**
         * 获取当前计算的MD5哈希值
         */
        public String getMD5Hex() {
            return bytesToHex(md5.digest());
        }
        
        /**
         * 获取写入的字节数
         */
        public long getBytesWritten() {
            return bytesWritten;
        }
        
        /**
         * 重置哈希计算器（如果需要重新计算）
         */
        public void resetHash() {
            md5.reset();
            bytesWritten = 0;
        }
    }
    
    private static MessageDigest createMD5Digest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5");
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}