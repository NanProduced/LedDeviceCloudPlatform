package org.nan.cloud.terminal.api.common.redis;

import lombok.Data;

@Data
public class ShareKeys {

    /**
     * 指令追踪信息
     * oid + tid + commandId
     */
    public static final String TERMINAL_TRACE_ID_PATTERN = "cmd_%d_%d_%d";

    /* 终端信息缓存 */
    // 在线终端列表
    public static final String TERMINAL_ONLINE_KEY_PATTERN = "terminal:online:org:%d";
}
