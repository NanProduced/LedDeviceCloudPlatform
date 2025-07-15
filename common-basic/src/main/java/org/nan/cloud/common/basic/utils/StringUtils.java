package org.nan.cloud.common.basic.utils;

import java.security.SecureRandom;

public class StringUtils {

    /** 字符池：数字 + 大写字母 + 小写字母 */
    private static final String CHAR_POOL = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /** 安全的随机数生成器 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成组织角色name字段<p>
     * 生成格式为 "O{oid}_{6 位随机字符}" 的字符串
     *
     * @param oid 业务域 ID
     * @return 生成的随机字符串
     */
    public static String generateOrgRoleName(Long oid) {
        StringBuilder sb = new StringBuilder();
        sb.append("O").append(oid).append("_");
        for (int i = 0; i < 6; i++) {
            int idx = RANDOM.nextInt(CHAR_POOL.length());
            sb.append(CHAR_POOL.charAt(idx));
        }
        return sb.toString();
    }

    public static boolean isNotBlank(String value) {
        return org.apache.commons.lang3.StringUtils.isNotBlank(value);
    }
}
