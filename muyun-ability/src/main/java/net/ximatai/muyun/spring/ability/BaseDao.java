package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.EntityDao;
import net.ximatai.muyun.spring.common.model.EntityContract;

public interface BaseDao<T extends EntityContract, ID> extends EntityDao<T, ID> {
}
