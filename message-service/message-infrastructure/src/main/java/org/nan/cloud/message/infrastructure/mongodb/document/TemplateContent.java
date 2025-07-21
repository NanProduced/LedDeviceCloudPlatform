package org.nan.cloud.message.infrastructure.mongodb.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息模板内容文档
 * 
 * 存储消息模板的详细内容、变量定义和样式配置。
 * 与MySQL中的MessageTemplate形成关联，通过templateId进行关联查询。
 * 支持富文本模板、变量替换和多语言配置。
 * 
 * 业务场景：
 * - 设备告警消息模板
 * - 系统通知消息模板  
 * - 任务完成通知模板
 * - 营销推广消息模板
 * - 多语言消息模板
 * 
 * 索引设计：
 * - 单字段索引：templateId（唯一），organizationId
 * - 复合索引：organizationId + templateType（按类型查询）
 * - 复合索引：organizationId + isActive（查询启用模板）
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "template_contents")
@CompoundIndexes({
    @CompoundIndex(name = "idx_org_type", def = "{'organizationId': 1, 'templateType': 1}"),
    @CompoundIndex(name = "idx_org_active", def = "{'organizationId': 1, 'isActive': 1}"),
    @CompoundIndex(name = "idx_type_active", def = "{'templateType': 1, 'isActive': 1}")
})
public class TemplateContent {
    
    /**
     * MongoDB文档ID
     */
    @Id
    private String id;
    
    /**
     * 模板唯一标识
     * 与MySQL中的template_id字段对应
     */
    @Indexed(unique = true)
    @Field("templateId")
    private String templateId;
    
    /**
     * 组织ID
     * 用于多租户数据隔离
     */
    @Indexed
    @Field("organizationId")
    private String organizationId;
    
    /**
     * 模板类型
     */
    @Indexed
    @Field("templateType")
    private String templateType;
    
    /**
     * 模板名称
     * 冗余存储，便于快速显示
     */
    @Field("templateName")
    private String templateName;
    
    /**
     * 标题模板内容
     * 支持变量占位符，如：设备{{deviceName}}{{alertType}}告警
     */
    @Field("titleTemplate")
    private String titleTemplate;
    
    /**
     * 文本内容模板
     * 纯文本格式的消息模板
     */
    @Field("contentTemplate")
    private String contentTemplate;
    
    /**
     * 富文本内容模板
     * HTML格式的消息模板，支持样式和格式化
     */
    @Field("richContentTemplate")
    private TemplateRichContent richContentTemplate;
    
    /**
     * 模板变量定义
     * 定义模板中可用的变量及其类型、验证规则等
     */
    @Field("variables")
    private List<TemplateVariable> variables;
    
    /**
     * 模板样式配置
     * 定义消息显示的样式，如颜色、字体等
     */
    @Field("styleConfig")
    private TemplateStyleConfig styleConfig;
    
    /**
     * 多语言支持
     * 不同语言版本的模板内容
     */
    @Field("localization")
    private Map<String, TemplateLocalization> localization;
    
    /**
     * 模板预设值
     * 常用的变量默认值配置
     */
    @Field("presetValues")
    private Map<String, Object> presetValues;
    
    /**
     * 模板使用示例
     * 展示模板渲染后的效果
     */
    @Field("examples")
    private List<TemplateExample> examples;
    
    /**
     * 模板验证规则
     * 用于验证模板语法和变量使用是否正确
     */
    @Field("validationRules")
    private TemplateValidationRules validationRules;
    
    /**
     * 是否启用
     * 控制模板是否可用于消息发送
     */
    @Indexed
    @Field("isActive")
    private Boolean isActive;
    
    /**
     * 模板版本号
     */
    @Field("version")
    private String version;
    
    /**
     * 创建时间
     */
    @Indexed
    @Field("createdTime")
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    @Field("updatedTime")
    private LocalDateTime updatedTime;
    
    /**
     * 富文本内容内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateRichContent {
        
        /**
         * HTML模板内容
         */
        private String htmlTemplate;
        
        /**
         * CSS样式定义
         */
        private String cssStyles;
        
        /**
         * 内联样式标记
         * 是否将CSS样式内联到HTML中
         */
        private Boolean inlineStyles;
        
        /**
         * 支持的HTML标签白名单
         */
        private List<String> allowedHtmlTags;
    }
    
    /**
     * 模板变量内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateVariable {
        
        /**
         * 变量名称
         */
        private String name;
        
        /**
         * 变量显示名称
         */
        private String displayName;
        
        /**
         * 变量类型
         * string, number, boolean, date, object等
         */
        private String type;
        
        /**
         * 是否必需
         */
        private Boolean required;
        
        /**
         * 默认值
         */
        private Object defaultValue;
        
        /**
         * 变量描述
         */
        private String description;
        
        /**
         * 验证规则
         */
        private Map<String, Object> validation;
        
        /**
         * 变量格式化规则
         * 如日期格式、数字格式等
         */
        private String format;
        
        /**
         * 枚举值列表（适用于选择类型变量）
         */
        private List<Object> enumValues;
    }
    
    /**
     * 模板样式配置内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateStyleConfig {
        
        /**
         * 主题颜色
         */
        private String primaryColor;
        
        /**
         * 背景颜色
         */
        private String backgroundColor;
        
        /**
         * 文字颜色
         */
        private String textColor;
        
        /**
         * 字体家族
         */
        private String fontFamily;
        
        /**
         * 字体大小
         */
        private String fontSize;
        
        /**
         * 行高
         */
        private String lineHeight;
        
        /**
         * 边框样式
         */
        private String borderStyle;
        
        /**
         * 图标配置
         */
        private Map<String, String> iconConfig;
        
        /**
         * 布局配置
         */
        private Map<String, Object> layoutConfig;
    }
    
    /**
     * 多语言配置内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateLocalization {
        
        /**
         * 语言代码
         * zh-CN, en-US等
         */
        private String language;
        
        /**
         * 本地化的标题模板
         */
        private String titleTemplate;
        
        /**
         * 本地化的内容模板
         */
        private String contentTemplate;
        
        /**
         * 本地化的变量显示名称
         */
        private Map<String, String> variableDisplayNames;
        
        /**
         * 本地化的预设值
         */
        private Map<String, Object> presetValues;
    }
    
    /**
     * 模板使用示例内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateExample {
        
        /**
         * 示例名称
         */
        private String name;
        
        /**
         * 示例描述
         */
        private String description;
        
        /**
         * 示例变量值
         */
        private Map<String, Object> sampleVariables;
        
        /**
         * 渲染后的标题
         */
        private String renderedTitle;
        
        /**
         * 渲染后的内容
         */
        private String renderedContent;
    }
    
    /**
     * 模板验证规则内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateValidationRules {
        
        /**
         * 最大模板长度
         */
        private Integer maxLength;
        
        /**
         * 最小模板长度
         */
        private Integer minLength;
        
        /**
         * 允许的变量正则表达式
         */
        private String variablePattern;
        
        /**
         * 禁用的关键词列表
         */
        private List<String> forbiddenKeywords;
        
        /**
         * 是否允许HTML标签
         */
        private Boolean allowHtmlTags;
        
        /**
         * 最大变量数量
         */
        private Integer maxVariableCount;
    }
    
    /**
     * 根据变量值渲染标题
     * 
     * @param variables 变量值映射
     * @return 渲染后的标题
     */
    public String renderTitle(Map<String, Object> variables) {
        return renderTemplate(titleTemplate, variables);
    }
    
    /**
     * 根据变量值渲染内容
     * 
     * @param variables 变量值映射
     * @return 渲染后的内容
     */
    public String renderContent(Map<String, Object> variables) {
        return renderTemplate(contentTemplate, variables);
    }
    
    /**
     * 模板渲染核心方法
     * 
     * @param template 模板字符串
     * @param variables 变量值映射
     * @return 渲染后的字符串
     */
    private String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    /**
     * 检查模板是否包含所需变量
     * 
     * @param requiredVariables 必需的变量列表
     * @return 是否包含所有必需变量
     */
    public boolean hasAllRequiredVariables(List<String> requiredVariables) {
        if (requiredVariables == null || requiredVariables.isEmpty()) {
            return true;
        }
        
        String fullTemplate = (titleTemplate != null ? titleTemplate : "") + 
                             (contentTemplate != null ? contentTemplate : "");
        
        return requiredVariables.stream()
                .allMatch(var -> fullTemplate.contains("{{" + var + "}}"));
    }
    
    /**
     * 获取模板中使用的所有变量
     * 
     * @return 变量名称列表
     */
    public List<String> extractUsedVariables() {
        String fullTemplate = (titleTemplate != null ? titleTemplate : "") + 
                             (contentTemplate != null ? contentTemplate : "");
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(fullTemplate);
        
        List<String> variables = new java.util.ArrayList<>();
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        
        return variables.stream().distinct().collect(java.util.stream.Collectors.toList());
    }
}