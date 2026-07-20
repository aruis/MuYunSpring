package net.ximatai.muyun.spring.dynamic.runtime;

public final class DynamicEntityServiceTestFactory {
    private DynamicEntityServiceTestFactory() {
    }

    public static DynamicEntityService forDataAccess(DynamicRecordDao dao, String moduleAlias) {
        return new DynamicEntityService(dao, moduleAlias);
    }
}
