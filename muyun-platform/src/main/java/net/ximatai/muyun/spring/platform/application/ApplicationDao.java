package net.ximatai.muyun.spring.platform.application;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface ApplicationDao extends BaseDao<Application, String> {
}
