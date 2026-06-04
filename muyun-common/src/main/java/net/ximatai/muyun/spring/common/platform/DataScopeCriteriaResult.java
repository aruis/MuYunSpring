package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.database.core.orm.Criteria;

public record DataScopeCriteriaResult(
        Criteria criteria,
        boolean restricted,
        boolean crossTenant
) {
    public DataScopeCriteriaResult {
        criteria = criteria == null ? Criteria.of() : criteria;
    }

    public DataScopeCriteriaResult(Criteria criteria, boolean restricted) {
        this(criteria, restricted, false);
    }

    public static DataScopeCriteriaResult unrestricted(Criteria criteria) {
        return new DataScopeCriteriaResult(criteria, false);
    }

    public static DataScopeCriteriaResult restricted(Criteria criteria) {
        return new DataScopeCriteriaResult(criteria, true);
    }

    public static DataScopeCriteriaResult crossTenantUnrestricted(Criteria criteria) {
        return new DataScopeCriteriaResult(criteria, false, true);
    }

    public static DataScopeCriteriaResult crossTenantRestricted(Criteria criteria) {
        return new DataScopeCriteriaResult(criteria, true, true);
    }
}
