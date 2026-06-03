package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.util.Optional;

public class AllowAllDataScopeCriteriaService implements DataScopeCriteriaService {
    @Override
    public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                    String actionCode,
                                                    Criteria criteria,
                                                    Optional<CurrentUser> currentUser) {
        return DataScopeCriteriaResult.unrestricted(criteria == null ? Criteria.of() : criteria);
    }

    @Override
    public Criteria applyReadScope(String moduleAlias,
                                   String actionCode,
                                   Criteria criteria,
                                   Optional<CurrentUser> currentUser) {
        return criteria == null ? Criteria.of() : criteria;
    }
}
