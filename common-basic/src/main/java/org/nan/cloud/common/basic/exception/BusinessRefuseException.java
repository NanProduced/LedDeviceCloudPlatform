package org.nan.cloud.common.basic.exception;

import lombok.Getter;

import java.io.Serial;

public class BusinessRefuseException extends BaseException{

    @Serial
    private static final long serialVersionUID = 3280764355454768140L;

    @Getter
    private Object data;

    public BusinessRefuseException(ExceptionEnum exceptionEnum, String msg, Object data) {
        super(exceptionEnum, msg);
        this.data = data;
    }
}
