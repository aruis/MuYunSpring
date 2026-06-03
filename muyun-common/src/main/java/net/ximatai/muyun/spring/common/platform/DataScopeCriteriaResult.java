package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.database.core.orm.Criteria;

public record DataScopeCriteriaResult(
        Criteria criteria,
        boolean restricted
) {
    public DataScopeCriteriaResult {
        criteria = criteria == null ? Criteria.of() : criteria;
    }

    public static DataScopeCriteriaResult unrestricted(Criteria criteria) {
        return new DataScopeCriteriaResult(criteria, false);
    }

    public static DataScopeCriteriaResult restricted(Criteria criteria) {
        return new DataScopeCriteriaResult(criteria, true);
    }
}
