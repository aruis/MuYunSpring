package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.util.Optional;

public interface DataScopeCriteriaService {
    DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                             String actionCode,
                                             Criteria criteria,
                                             Optional<CurrentUser> currentUser);

    DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                             ActionExecutionPolicy policy,
                                             Criteria criteria,
                                             Optional<CurrentUser> currentUser);

    default Criteria applyReadScope(String moduleAlias,
                                    String actionCode,
                                    Criteria criteria,
                                    Optional<CurrentUser> currentUser) {
        return resolveReadScope(moduleAlias, actionCode, criteria, currentUser).criteria();
    }
}
