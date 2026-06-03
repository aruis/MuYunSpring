package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.util.Optional;

public interface DataScopeCriteriaService {
    default DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                     String actionCode,
                                                     Criteria criteria,
                                                     Optional<CurrentUser> currentUser) {
        Criteria scoped = applyReadScope(moduleAlias, actionCode, criteria, currentUser);
        return new DataScopeCriteriaResult(scoped, scoped != criteria);
    }

    Criteria applyReadScope(String moduleAlias,
                            String actionCode,
                            Criteria criteria,
                            Optional<CurrentUser> currentUser);
}
