package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface CodeValueMappingDao extends BaseDao<CodeValueMapping, String> {
}
