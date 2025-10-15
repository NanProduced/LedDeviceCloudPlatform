package org.nan.cloud.common.mq.core.serializer;

import org.nan.cloud.common.mq.core.message.Message;

/**
 * 消息序列化接口
 * 
 * 定义消息序列化和反序列化的统一接口。
 * 支持多种序列化方式的扩展。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface MessageSerializer {
    
    /**
     * 序列化消息
     * 
     * @param message 消息对象
     * @return 序列化后的字节数组
     * @throws Exception 序列化异常
     */
    byte[] serialize(Message message) throws Exception;
    
    /**
     * 反序列化消息
     * 
     * @param data 序列化数据
     * @return 消息对象
     * @throws Exception 反序列化异常
     */
    Message deserialize(byte[] data) throws Exception;
    
    /**
     * 序列化任意对象
     * 
     * @param object 待序列化对象
     * @return 序列化后的字节数组
     * @throws Exception 序列化异常
     */
    byte[] serializeObject(Object object) throws Exception;
    
    /**
     * 反序列化任意对象
     * 
     * @param data 序列化数据
     * @param clazz 目标类型
     * @return 反序列化后的对象
     * @throws Exception 反序列化异常
     */
    <T> T deserializeObject(byte[] data, Class<T> clazz) throws Exception;
    
    /**
     * 获取序列化类型标识
     * 
     * @return 序列化类型
     */
    String getType();
    
    /**
     * 是否支持压缩
     * 
     * @return true表示支持压缩
     */
    boolean supportsCompression();
}