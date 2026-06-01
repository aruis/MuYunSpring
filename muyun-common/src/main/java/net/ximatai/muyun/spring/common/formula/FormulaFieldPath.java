package net.ximatai.muyun.spring.common.formula;

public record FormulaFieldPath(String tableKey, String fieldName) {
    public FormulaFieldPath {
        tableKey = tableKey == null || tableKey.isBlank() ? null : tableKey.trim();
        fieldName = fieldName == null ? "" : fieldName.trim();
    }

    public static FormulaFieldPath parse(String dataIndex) {
        if (dataIndex == null || dataIndex.isBlank()) {
            return new FormulaFieldPath(null, "");
        }
        int dot = dataIndex.indexOf('.');
        if (dot <= 0) {
            return new FormulaFieldPath(null, dataIndex);
        }
        return new FormulaFieldPath(dataIndex.substring(0, dot), dataIndex.substring(dot + 1));
    }

    public String dataIndex() {
        return tableKey == null ? fieldName : tableKey + "." + fieldName;
    }
}
