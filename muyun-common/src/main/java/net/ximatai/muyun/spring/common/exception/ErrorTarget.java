package net.ximatai.muyun.spring.common.exception;

public record ErrorTarget(String kind,
                          String moduleAlias,
                          String entityAlias,
                          String relationAlias,
                          String fieldName,
                          Integer rowIndex,
                          String recordId,
                          String actionCode,
                          String attachmentId) {
    public static ErrorTarget field(String fieldName) {
        return new ErrorTarget("field", null, null, null, fieldName, null, null, null, null);
    }

    public static ErrorTarget record(String recordId) {
        return new ErrorTarget("record", null, null, null, null, null, recordId, null, null);
    }

    public static ErrorTarget action(String actionCode) {
        return new ErrorTarget("action", null, null, null, null, null, null, actionCode, null);
    }

    public static ErrorTarget attachment(String attachmentId) {
        return new ErrorTarget("attachment", null, null, null, null, null, null, null, attachmentId);
    }

    public ErrorTarget module(String moduleAlias) {
        return new ErrorTarget(kind, moduleAlias, entityAlias, relationAlias, fieldName, rowIndex, recordId,
                actionCode, attachmentId);
    }

    public ErrorTarget entity(String entityAlias) {
        return new ErrorTarget(kind, moduleAlias, entityAlias, relationAlias, fieldName, rowIndex, recordId,
                actionCode, attachmentId);
    }

    public ErrorTarget relation(String relationAlias) {
        return new ErrorTarget(kind, moduleAlias, entityAlias, relationAlias, fieldName, rowIndex, recordId,
                actionCode, attachmentId);
    }

    public ErrorTarget row(Integer rowIndex) {
        return new ErrorTarget(kind, moduleAlias, entityAlias, relationAlias, fieldName, rowIndex, recordId,
                actionCode, attachmentId);
    }
}
