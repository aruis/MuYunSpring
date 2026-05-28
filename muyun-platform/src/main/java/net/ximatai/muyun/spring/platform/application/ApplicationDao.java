package net.ximatai.muyun.spring.platform.application;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface ApplicationDao extends BaseDao<Application, String> {
}
