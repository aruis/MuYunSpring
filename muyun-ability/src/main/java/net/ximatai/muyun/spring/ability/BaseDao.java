package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.EntityDao;
import net.ximatai.muyun.spring.common.model.EntityContract;

import java.util.Map;

public interface BaseDao<T extends EntityContract, ID> extends EntityDao<T, ID> {
    default int updateByIdAndVersion(T entity, Integer expectedVersion) {
        if (expectedVersion == null) {
            throw new IllegalArgumentException("expectedVersion must not be null");
        }
        return updateByIdAndCondition(entity, Map.of("version", expectedVersion));
    }

    default int deleteByIdAndVersion(ID id, Integer expectedVersion) {
        if (expectedVersion == null) {
            throw new IllegalArgumentException("expectedVersion must not be null");
        }
        return deleteByIdAndCondition(id, Map.of("version", expectedVersion));
    }
}
