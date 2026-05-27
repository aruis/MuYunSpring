package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.DefaultSimpleEntityManager;
import net.ximatai.muyun.database.core.orm.EntityMetaResolver;
import net.ximatai.muyun.database.core.orm.SimpleEntityManager;

public final class PlatformEntityManagers {
    private PlatformEntityManagers() {
    }

    public static EntityMetaResolver entityMetaResolver() {
        StaticEntityTableMapper mapper = new StaticEntityTableMapper();
        return new EntityMetaResolver(mapper::toTable);
    }

    public static SimpleEntityManager simpleEntityManager(IDatabaseOperations<?> operations,
                                                          EntityMetaResolver entityMetaResolver) {
        return new DefaultSimpleEntityManager(operations, entityMetaResolver);
    }
}
