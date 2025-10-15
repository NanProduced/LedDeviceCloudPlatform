package org.nan.cloud.file.infrastructure.vsn;

/**
 * VSN 格式工具：颜色与布尔值规范化
 */
public final class VsnFormatUtils {

    private VsnFormatUtils() {}

    /**
     * 将 Integer 颜色值格式化为 #AARRGGBB
     */
    public static String colorFromInt(Integer color) {
        if (color == null) return null;
        long unsigned = color & 0xFFFFFFFFL;
        return String.format("#%08X", unsigned);
    }

    /**
     * 将字符串颜色规范化为 #AARRGGBB
     * 支持形如：#RRGGBB / #AARRGGBB / 0xAARRGGBB / 0xRRGGBB / 纯8或6位十六进制
     */
    public static String ensureColorString(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        // 去除前缀
        if (v.startsWith("0x") || v.startsWith("0X")) v = v.substring(2);
        if (v.startsWith("#")) v = v.substring(1);
        // 只保留十六进制
        v = v.replaceAll("[^0-9A-Fa-f]", "");
        if (v.length() == 6) {
            // 默认不带Alpha，补FF
            v = "FF" + v;
        }
        if (v.length() != 8) {
            // 无法识别，返回原始
            return s;
        }
        long val;
        try {
            val = Long.parseUnsignedLong(v, 16);
        } catch (NumberFormatException e) {
            return s;
        }
        return String.format("#%08X", val);
    }

    /**
     * 归一化布尔值为 0/1 字符串
     */
    public static String ensure01(String v) {
        if (v == null) return null;
        String t = v.trim().toLowerCase();
        if (t.equals("1") || t.equals("true") || t.equals("yes") || t.equals("y")) return "1";
        if (t.equals("0") || t.equals("false") || t.equals("no") || t.equals("n")) return "0";
        // 非常规，原样返回，避免误改
        return v;
    }
}

