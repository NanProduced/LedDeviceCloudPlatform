package org.nan.cloud.core.exception;

import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;

public class BusinessException extends BaseException {
    public BusinessException(Throwable throwable, String msg) {
        super(throwable, msg);
    }

    public BusinessException(ExceptionEnum exceptionEnum, String msg) {
        super(exceptionEnum, msg);
    }
}
