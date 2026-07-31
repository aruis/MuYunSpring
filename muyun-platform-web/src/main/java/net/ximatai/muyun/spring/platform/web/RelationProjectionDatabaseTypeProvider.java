package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.database.core.metadata.DBInfo;
import org.springframework.stereotype.Component;

@Component
public class RelationProjectionDatabaseTypeProvider {
    public DBInfo.Type databaseType() {
        return DBInfo.Type.POSTGRESQL;
    }
}
