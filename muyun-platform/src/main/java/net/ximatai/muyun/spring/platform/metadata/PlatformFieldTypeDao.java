package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.database.quarkus.MuYunRepository;

@MuYunRepository
public interface PlatformFieldTypeDao extends BaseDao<PlatformFieldType, String> {
}
