package org.nan.cloud.auth.boot.oidc.enums;

public enum LoginStateEnum {

    LOGIN(1, "已登录"),
    LOGOUT(2, "已登出"),
    EXPIRED(3, "已过期");

    /**
     * 登录状态
     */
    private Integer code;
    /**
     * 登录状态描述
     */
    private String desc;

    LoginStateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
