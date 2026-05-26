package net.ximatai.muyun.spring.common.model;

import net.ximatai.muyun.spring.common.id.Ids;
import net.ximatai.muyun.spring.common.tenant.TenantContext;

import java.time.Instant;

public final class EntityLifecycle {
    private EntityLifecycle() {
    }

    public static void prepareInsert(EntityContract model, Instant now) {
        if (model.getId() == null || model.getId().isBlank()) {
            model.setId(Ids.newId());
        }
        TenantContext.applyToNewEntity(model);
        model.setVersion(model.getVersion() == null ? 0 : model.getVersion());
        model.setDeleted(Boolean.FALSE);
        model.setCreatedAt(model.getCreatedAt() == null ? now : model.getCreatedAt());
        model.setUpdatedAt(now);
    }

    public static void prepareUpdate(EntityContract model, Instant now) {
        prepareUpdate(model, now, nextVersion(model.getVersion()));
    }

    public static void prepareUpdate(EntityContract model, Instant now, Integer nextVersion) {
        model.setUpdatedAt(now);
        model.setVersion(nextVersion);
    }

    public static void prepareDelete(EntityContract model, Instant now) {
        model.setDeleted(Boolean.TRUE);
        prepareUpdate(model, now);
    }

    public static Integer nextVersion(Integer currentVersion) {
        return currentVersion == null ? 1 : currentVersion + 1;
    }
}
