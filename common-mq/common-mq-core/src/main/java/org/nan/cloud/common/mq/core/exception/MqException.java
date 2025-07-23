package org.nan.cloud.common.mq.core.exception;

/**
 * 消息队列异常基类
 * 
 * 定义消息队列操作过程中的通用异常类型。
 * 
 * @author LedDeviceCloudPlatform Team  
 * @since 1.0.0
 */
public class MqException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误代码
     */
    private final String errorCode;
    
    /**
     * 业务数据
     */
    private final Object data;
    
    public MqException(String message) {
        super(message);
        this.errorCode = "MQ_ERROR";
        this.data = null;
    }
    
    public MqException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.data = null;
    }
    
    public MqException(String errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }
    
    public MqException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "MQ_ERROR";
        this.data = null;
    }
    
    public MqException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.data = null;
    }
    
    public MqException(String errorCode, String message, Object data, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.data = data;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Object getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return String.format("MqException[errorCode=%s, message=%s, data=%s]", 
                errorCode, getMessage(), data);
    }
}