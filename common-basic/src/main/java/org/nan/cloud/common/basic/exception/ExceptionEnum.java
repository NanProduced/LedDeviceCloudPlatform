package org.nan.cloud.common.basic.exception;

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
    CREATE_FAILED(1013, "create failed"),
    UPDATE_FAILED(1014, "update failed"),
    RPC_REQUEST_FAILED(1015, "RPC Request failed"),

    /* 参数校验 -> 4xxx */
    USER_NAME_DUPLICATION_EXCEPTION(4001, "Username is duplication"),
    GROUP_HAS_SUB_GROUP(4002, "Group has sub group"),

    /* 业务异常 -> 5xxx */
    USER_PASSWORD_NOT_MATCH(5001, "User password not match"),
    USER_GROUP_INIT_FAILED(5002, "User group init failed"),
    USER_GROUP_PERMISSION_DENIED(5003, "User group permission denied"),
    USER_PERMISSION_DENIED(5004, "User permission denied"),
    ORG_PERMISSION_DENIED(5005, "Wrong organization permission"),
    SAME_USERNAME(5006, "There is already a same username"),
    ROLE_DOES_NOT_EXIST(5007, "Role doesn't exist"),
    ROLE_PERMISSION_DENIED(5008, "Doesn't have enough role permissions"),
    NAME_DUPLICATE_ERROR(5009, "Already has the same name"),
    HAS_USER_WITH_ONLY_ROLE(5010, "There are users with only role"),
    HAS_USER_IN_TARGET_GROUP(5011, "There are users in target group"),
    TERMINAL_GROUP_PERMISSION_DENIED(5012, "terminal group permission denied"),
    CREATE_TERMINAL_ACCOUNT_FAILED(5012, "create terminal account failed"),
    OPERATION_PERMISSION_DENIED(5013, "operation permission failed"),
    HAS_DUPLICATE_FOLDER_NAME(5014, "There is already a duplicate folder name"),
    FOLDER_PERMISSION_DENIED(5015, "Folder permission denied"),
    COMMAND_PARAMS_ERROR(5016, "command parameters error"),

    /* websocket\STOMP 异常 */
    STOMP_ACCESS_DENIED(6001, "stomp access denied"),


    /* RabbitMq异常 */
    UNKNOWN_ROUTING_KEY(6002, "unknown routing key"),
    UNKNOWN_MQ_MESSAGE_TYPE(6003, "unknown message type");


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
