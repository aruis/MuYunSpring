package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.database.core.orm.Criteria;

public record TreeScope(Criteria criteria, String tenantId) {
    public TreeScope {
        criteria = criteria == null ? Criteria.of() : criteria;
        tenantId = tenantId == null || tenantId.isBlank() ? null : tenantId.trim();
    }

    public static TreeScope of(Criteria criteria) {
        return new TreeScope(criteria, null);
    }

    public static TreeScope tenant(Criteria criteria, String tenantId) {
        return new TreeScope(criteria, tenantId);
    }

    public static TreeScope none() {
        return new TreeScope(Criteria.of(), null);
    }
}
