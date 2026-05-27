package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.EntityDao;
import net.ximatai.muyun.spring.common.model.EntityContract;

public interface BaseDao<T extends EntityContract, ID> extends EntityDao<T, ID> {
    /**
     * Contract fallback for version-aware updates.
     *
     * This default implementation is not a database-atomic CAS operation because it reads before writing.
     * Real database DAOs should override it with a single conditional update statement.
     */
    default int updateByIdAndVersion(T entity, Integer expectedVersion) {
        if (expectedVersion == null) {
            return updateById(entity);
        }
        @SuppressWarnings("unchecked")
        ID id = (ID) entity.getId();
        T existing = findById(id);
        if (existing == null || !expectedVersion.equals(existing.getVersion())) {
            return 0;
        }
        return updateById(entity);
    }

    /**
     * Contract fallback for version-aware deletes.
     *
     * This default implementation is not a database-atomic CAS operation because it reads before deleting.
     * Real database DAOs should override it with a single conditional delete statement.
     */
    default int deleteByIdAndVersion(ID id, Integer expectedVersion) {
        if (expectedVersion == null) {
            return deleteById(id);
        }
        T existing = findById(id);
        if (existing == null || !expectedVersion.equals(existing.getVersion())) {
            return 0;
        }
        return deleteById(id);
    }
}
