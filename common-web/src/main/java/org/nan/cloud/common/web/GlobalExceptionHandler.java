package org.nan.cloud.common.web;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.BusinessRefuseException;
import org.nan.cloud.common.basic.exception.Terminal400Exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<?> handleBaseException(BaseException e, HttpServletRequest request) {
        log.error("BaseException: {}", e.getMsg(), e);
        
        // 检查请求的Content-Type或Accept头，如果是图片预览请求则不返回JSON
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getContentType();
        String requestURI = request.getRequestURI();
        
        // 如果是文件预览相关请求且期望的是图片内容，则返回简单错误响应
        if ((acceptHeader != null && acceptHeader.contains("image/")) 
            || (contentType != null && contentType.startsWith("image/"))
            || requestURI.contains("/preview/") || requestURI.contains("/thumbnail/")) {
            
            log.warn("文件预览请求失败，返回HTTP错误状态 - URI: {}, 错误: {}", requestURI, e.getMsg());
            return new ResponseEntity<>(e.getHttpStatus());
        }
        
        // 其他请求返回JSON格式错误信息
        return new ResponseEntity<>(DynamicResponse.fail(null, e.getErrorCode(), e.getMsg()), e.getHttpStatus());
    }

    @ExceptionHandler(BusinessRefuseException.class)
    public ResponseEntity<DynamicResponse<?>> handleBusinessRefuseException(BusinessRefuseException e) {
        log.error("BusinessRefuseException: data: {}, {}", e.getData().toString(), e.getMsg());
        return new ResponseEntity<>(DynamicResponse.fail(e.getData().toString(), e.getErrorCode(), e.getMsg()),  e.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
        return handleBaseException(new BaseException(e, e.getMessage()), request);
    }

    @ExceptionHandler(Terminal400Exception.class)
    public ResponseEntity<Void> handleTerminal400Exception(Terminal400Exception e) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }


}
