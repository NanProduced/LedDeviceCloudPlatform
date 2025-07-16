package org.nan.cloud.common.web;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.BusinessRefuseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<DynamicResponse<?>> handleBaseException(BaseException e) {
        log.error("BaseException: {}", e.getMsg(), e);
        return new ResponseEntity<>(DynamicResponse.fail(null, e.getErrorCode(), e.getMsg()), e.getHttpStatus());

    }

    @ExceptionHandler(BusinessRefuseException.class)
    public ResponseEntity<DynamicResponse<?>> handleBusinessRefuseException(BusinessRefuseException e) {
        log.error("BusinessRefuseException: data: {}, {}", e.getData().toString(), e.getMsg());
        return new ResponseEntity<>(DynamicResponse.fail(e.getData().toString(), e.getErrorCode(), e.getMsg()),  e.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DynamicResponse<?>> handleException(Exception e) {
        return handleBaseException(new BaseException(e, e.getMessage()));
    }


}
