package org.nan.cloud.terminal.api.common.redis;

import lombok.Data;

@Data
public class ShareKeys {

    /* 终端信息缓存 */
    // 在线终端列表
    public static final String TERMINAL_ONLINE_KEY_PATTERN = "terminal:online:org:%d";
}
