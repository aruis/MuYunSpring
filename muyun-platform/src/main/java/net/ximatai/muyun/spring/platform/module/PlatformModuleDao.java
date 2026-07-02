package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface PlatformModuleDao extends BaseDao<PlatformModule, String> {
}
