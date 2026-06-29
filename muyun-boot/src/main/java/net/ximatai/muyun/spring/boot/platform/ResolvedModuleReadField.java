package net.ximatai.muyun.spring.boot.platform;

public record ResolvedModuleReadField(String entityAlias,
                                      String relationCode,
                                      String fieldName,
                                      boolean platformManaged) {
    public ResolvedModuleReadField {
        entityAlias = entityAlias == null || entityAlias.isBlank() ? null : entityAlias.trim();
        relationCode = relationCode == null || relationCode.isBlank() ? null : relationCode.trim();
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("resolved module read field name must not be blank");
        }
        fieldName = fieldName.trim();
    }
}
