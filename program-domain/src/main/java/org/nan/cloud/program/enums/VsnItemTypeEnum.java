package org.nan.cloud.program.enums;

import lombok.Getter;

import java.util.Set;

/**
 * VSN节目项类型枚举
 * 严格按照.doc/节目vsn格式.md中的ItemType定义
 */
@Getter
public enum VsnItemTypeEnum {

    /**
     * 图片类型
     */
    IMAGE("2", "图片", Set.of("IMAGE")),

    /**
     * 视频类型
     */
    VIDEO("3", "视频", Set.of("VIDEO")),

    /**
     * 单行文本
     */
    SINGLE_LINE_TEXT("4", "单行文本", Set.of("TEXT")),

    /**
     * 多行文本（txt文件等）
     */
    MULTI_LINE_TEXT("5", "多行文本", Set.of("TEXT")),

    /**
     * GIF动画
     */
    GIF("6", "GIF", Set.of("IMAGE")),

    /**
     * 电视卡/采集卡/摄像头
     */
    TV_CARD("8", "电视卡/采集卡/摄像头", Set.of("DEVICE")),

    /**
     * 普通时钟
     */
    CLOCK("9", "普通时钟", Set.of("VIRTUAL")),

    /**
     * 中国气象、全球气象
     */
    WEATHER("14", "天气信息", Set.of("VIRTUAL")),

    /**
     * 计时器
     */
    TIMER("15", "计时器", Set.of("VIRTUAL")),

    /**
     * 精美时钟
     */
    EXQUISITE_CLOCK("16", "精美时钟", Set.of("VIRTUAL")),

    /**
     * 湿度传感器
     */
    HUMIDITY("21", "湿度", Set.of("SENSOR")),

    /**
     * 温度传感器
     */
    TEMPERATURE("22", "温度", Set.of("SENSOR")),

    /**
     * 噪音传感器
     */
    NOISE("23", "噪音", Set.of("SENSOR")),

    /**
     * 空气质量传感器
     */
    AIR_QUALITY("24", "空气质量", Set.of("SENSOR")),

    /**
     * 网页、流媒体
     */
    WEB_STREAM("27", "网页/流媒体", Set.of("URL")),

    /**
     * 烟雾传感器
     */
    SMOKE("28", "烟雾", Set.of("SENSOR")),

    /**
     * 显示没有传感器时的提示文字
     */
    SENSOR_PROMPT("29", "传感器提示", Set.of("VIRTUAL")),

    /**
     * 传感器初始值
     */
    SENSOR_INITIAL("30", "传感器初始值", Set.of("SENSOR")),

    /**
     * 单列文本
     */
    SINGLE_COLUMN_TEXT("102", "单列文本", Set.of("TEXT"));

    /**
     * VSN格式中的type值
     */
    private final String vsnTypeCode;

    /**
     * 显示名称
     */
    private final String displayName;

    /**
     * 支持的素材类型
     */
    private final Set<String> supportedMaterialTypes;

    VsnItemTypeEnum(String vsnTypeCode, String displayName, Set<String> supportedMaterialTypes) {
        this.vsnTypeCode = vsnTypeCode;
        this.displayName = displayName;
        this.supportedMaterialTypes = supportedMaterialTypes;
    }

    /**
     * 根据VSN类型代码查找枚举
     */
    public static VsnItemTypeEnum fromVsnCode(String vsnTypeCode) {
        for (VsnItemTypeEnum type : values()) {
            if (type.vsnTypeCode.equals(vsnTypeCode)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的VSN类型代码: " + vsnTypeCode);
    }

    /**
     * 检查是否支持指定的素材类型
     */
    public boolean supportsMaterialType(String materialType) {
        return supportedMaterialTypes.contains(materialType);
    }
}