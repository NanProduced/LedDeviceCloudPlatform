package org.nan.cloud.common.mq.core.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.message.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * JSON消息序列化器
 * 
 * 使用Jackson库进行JSON序列化，支持Java 8时间类型，
 * 可选择是否启用压缩功能。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
public class JsonMessageSerializer implements MessageSerializer {
    
    private final ObjectMapper objectMapper;
    private final boolean compressionEnabled;
    private final int compressionThreshold;
    
    public JsonMessageSerializer() {
        this(false, 1024);
    }
    
    public JsonMessageSerializer(boolean compressionEnabled, int compressionThreshold) {
        this.objectMapper = createObjectMapper();
        this.compressionEnabled = compressionEnabled;
        this.compressionThreshold = compressionThreshold;
    }
    
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 支持Java 8时间类型
        mapper.registerModule(new JavaTimeModule());
        // 忽略未知属性
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 包含空值
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        return mapper;
    }
    
    @Override
    public byte[] serialize(Message message) throws Exception {
        try {
            String json = objectMapper.writeValueAsString(message);
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            
            // 根据阈值判断是否压缩
            if (compressionEnabled && data.length > compressionThreshold) {
                data = compress(data);
                log.debug("消息已压缩: messageId={}, 原始大小={}, 压缩后大小={}", 
                        message.getMessageId(), json.length(), data.length);
            }
            
            return data;
        } catch (Exception e) {
            log.error("消息序列化失败: messageId={}, error={}", message.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public Message deserialize(byte[] data) throws Exception {
        try {
            // 尝试解压缩
            byte[] actualData = data;
            if (compressionEnabled && isCompressed(data)) {
                actualData = decompress(data);
                log.debug("消息已解压缩: 压缩大小={}, 解压后大小={}", data.length, actualData.length);
            }
            
            String json = new String(actualData, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, Message.class);
        } catch (Exception e) {
            log.error("消息反序列化失败: dataLength={}, error={}", data.length, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public byte[] serializeObject(Object object) throws Exception {
        try {
            String json = objectMapper.writeValueAsString(object);
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            
            if (compressionEnabled && data.length > compressionThreshold) {
                data = compress(data);
                log.debug("对象已压缩: class={}, 原始大小={}, 压缩后大小={}", 
                        object.getClass().getSimpleName(), json.length(), data.length);
            }
            
            return data;
        } catch (Exception e) {
            log.error("对象序列化失败: class={}, error={}", object.getClass().getName(), e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public <T> T deserializeObject(byte[] data, Class<T> clazz) throws Exception {
        try {
            byte[] actualData = data;
            if (compressionEnabled && isCompressed(data)) {
                actualData = decompress(data);
            }
            
            String json = new String(actualData, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("对象反序列化失败: class={}, dataLength={}, error={}", 
                    clazz.getName(), data.length, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public String getType() {
        return "json";
    }
    
    @Override
    public boolean supportsCompression() {
        return compressionEnabled;
    }
    
    /**
     * GZIP压缩
     */
    private byte[] compress(byte[] data) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
            gzipOut.finish();
            return baos.toByteArray();
        }
    }
    
    /**
     * GZIP解压缩
     */
    private byte[] decompress(byte[] compressedData) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }
    
    /**
     * 简单判断数据是否被压缩
     * GZIP格式的前两个字节是0x1f, 0x8b
     */
    private boolean isCompressed(byte[] data) {
        return data.length >= 2 && 
               (data[0] & 0xFF) == 0x1f && 
               (data[1] & 0xFF) == 0x8b;
    }
}