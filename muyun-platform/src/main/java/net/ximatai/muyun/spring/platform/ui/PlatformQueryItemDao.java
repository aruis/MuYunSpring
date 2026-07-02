package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.database.quarkus.MuYunRepository;

@MuYunRepository
public interface PlatformQueryItemDao extends BaseDao<PlatformQueryItem, String> {
}
