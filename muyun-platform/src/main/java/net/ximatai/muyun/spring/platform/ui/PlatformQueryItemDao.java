package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;

@MuYunRepository
public interface PlatformQueryItemDao extends BaseDao<PlatformQueryItem, String> {
}
