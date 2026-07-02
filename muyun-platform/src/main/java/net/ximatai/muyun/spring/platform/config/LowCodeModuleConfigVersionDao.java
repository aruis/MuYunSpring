package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface LowCodeModuleConfigVersionDao extends BaseDao<LowCodeModuleConfigVersion, String> {
}
