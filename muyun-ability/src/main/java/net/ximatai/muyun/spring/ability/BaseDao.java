package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.EntityDao;
import net.ximatai.muyun.spring.common.model.BaseModel;

public interface BaseDao<T extends BaseModel, ID> extends EntityDao<T, ID> {
}
