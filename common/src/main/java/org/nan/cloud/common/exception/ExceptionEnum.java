package org.nan.cloud.common.exception;

import lombok.Getter;

import java.util.function.Supplier;

@Getter
public enum ExceptionEnum {

    /* 默认成功参数 */
    SUCCESS(200, "success"),
    /* 默认失败参数 */
    SERVER_ERROR(500, "server error"),


    /* 通用异常码 -> 1xxx */
    JSON_SERIALIZATION_EXCEPTION(1008, "JsonUtil serialization failed"),
    JSON_DESERIALIZATION_EXCEPTION(1009, "JsonUtil deserialization failed"),
    PERMISSION_DENIED(1010, "Permission denied"),
    PARAM_PARAMETER_EXCEPTION(1011, "param is invalid"),
    OPERATION_NOT_SUPPORTED(1012, "operation not supported"),

    /* 参数校验 -> 4xxx */
    USER_NAME_DUPLICATION_EXCEPTION(4001, "Username is duplication"),
    GROUP_HAS_SUB_GROUP(4002, "Group has sub group");

    private final Integer code;
    private final String message;

    ExceptionEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 多种方式抛出异常
     */
    public BaseException throwThis(Object... args) {
        throw new BaseException(this, args);
    }

    public Supplier<BaseException> throwSupplier(Object... args) {
        return () -> throwThis(args);
    }

    public void throwIf(boolean expr, Object... args) {
        if (expr) {
            throw throwThis(args);
        }
    }

    public void throwIf(boolean expr) {
        if (expr) {
            throw throwThis(this.message);
        }
    }
}
