package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface PlatformRecordNavigationSessionDao extends BaseDao<PlatformRecordNavigationSession, String> {
}
