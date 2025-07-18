package org.nan.cloud.core.DTO;

/**
 * 绑定类型枚举
 */
public enum BindingType {
    /**
     * 包含绑定 - 绑定该终端组及其子组（如果includeChildren=true）
     */
    INCLUDE,
    
    /**
     * 排除绑定 - 从父组的包含绑定中排除该终端组
     */
    EXCLUDE
}