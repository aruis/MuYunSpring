package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;

import java.util.List;

public final class PlatformFieldUiTypePresetCatalog {
    private PlatformFieldUiTypePresetCatalog() {
    }

    public static List<PlatformFieldUiType> fieldUiTypes() {
        return List.of(
                fieldUiType("text", "输入框", "string", ViewControlType.TEXT),
                fieldUiType("textarea", "文本域", "text", ViewControlType.TEXTAREA),
                fieldUiType("number", "数字", "decimal", ViewControlType.DECIMAL),
                fieldUiType("integer", "整数", "integer", ViewControlType.NUMBER),
                fieldUiType("amount", "金额", "decimal", ViewControlType.DECIMAL),
                fieldUiType("percentage", "百分比", "decimal", ViewControlType.DECIMAL),
                fieldUiType("switch", "开关", "boolean", ViewControlType.SWITCH),
                fieldUiType("select", "下拉单选", "string", ViewControlType.SELECT),
                fieldUiType("multi_select", "下拉多选", "json", ViewControlType.MULTI_SELECT),
                fieldUiType("date", "日期", "date", ViewControlType.DATE),
                fieldUiType("datetime", "日期时间", "datetime", ViewControlType.DATETIME),
                fieldUiType("date_time_with_time_zone", "日期时间（含时区）", "zoned_datetime", ViewControlType.DATETIME),
                fieldUiType("json", "JSON", "json", ViewControlType.JSON),
                fieldUiType("date_range", "日期区间", "date", ViewControlType.DATE),
                fieldUiType("date_time_range", "日期时间区间", "datetime", ViewControlType.DATETIME)
        );
    }

    public static List<PlatformFieldUiTypeAttribute> attributes() {
        return List.of(
                attribute("text", "maxLength", "字数限制", "integer", null),
                attribute("text", "placeholder", "占位提示", "string", null),
                attribute("textarea", "rows", "显示行数", "integer", "4"),
                attribute("textarea", "placeholder", "占位提示", "string", null),
                attribute("number", "precision", "小数位数", "integer", "2"),
                attribute("number", "min", "最小值", "decimal", null),
                attribute("number", "max", "最大值", "decimal", null),
                attribute("amount", "precision", "小数位数", "integer", "2"),
                attribute("amount", "min", "最小值", "decimal", "0"),
                attribute("percentage", "precision", "小数位数", "integer", "2"),
                attribute("percentage", "min", "最小值", "decimal", "0"),
                attribute("percentage", "max", "最大值", "decimal", "100"),
                attribute("date", "format", "格式", "string", "YYYY-MM-DD"),
                attribute("datetime", "format", "格式", "string", "YYYY-MM-DD HH:mm:ss"),
                attribute("date_time_with_time_zone", "format", "格式", "string", "YYYY-MM-DD HH:mm:ss")
        );
    }

    public static List<PlatformFieldUiTypeFieldMapping> fieldMappings() {
        return List.of(
                mapping("date_range", "end", "结束值"),
                mapping("date_time_range", "end", "结束值"),
                mapping("date_time_with_time_zone", "timeZone", "时区")
        );
    }

    private static PlatformFieldUiType fieldUiType(String alias,
                                                   String title,
                                                   String defaultFieldTypeAlias,
                                                   ViewControlType controlType) {
        PlatformFieldUiType type = new PlatformFieldUiType();
        type.setId(alias);
        type.setAlias(alias);
        type.setTitle(title);
        type.setDefaultFieldTypeAlias(defaultFieldTypeAlias);
        type.setControlType(controlType);
        return type;
    }

    private static PlatformFieldUiTypeAttribute attribute(String fieldUiTypeAlias,
                                                          String attributeAlias,
                                                          String title,
                                                          String valueFieldTypeAlias,
                                                          String defaultValue) {
        PlatformFieldUiTypeAttribute attribute = new PlatformFieldUiTypeAttribute();
        attribute.setFieldUiTypeAlias(fieldUiTypeAlias);
        attribute.setAttributeAlias(attributeAlias);
        attribute.setTitle(title);
        attribute.setValueFieldTypeAlias(valueFieldTypeAlias);
        attribute.setDefaultValue(defaultValue);
        return attribute;
    }

    private static PlatformFieldUiTypeFieldMapping mapping(String fieldUiTypeAlias, String sourceKey, String title) {
        PlatformFieldUiTypeFieldMapping mapping = new PlatformFieldUiTypeFieldMapping();
        mapping.setFieldUiTypeAlias(fieldUiTypeAlias);
        mapping.setSourceKey(sourceKey);
        mapping.setTitle(title);
        return mapping;
    }
}
