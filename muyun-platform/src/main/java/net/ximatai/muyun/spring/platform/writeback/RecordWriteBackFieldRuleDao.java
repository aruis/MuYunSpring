package net.ximatai.muyun.spring.platform.writeback;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.database.quarkus.MuYunRepository;

@MuYunRepository
public interface RecordWriteBackFieldRuleDao extends BaseDao<RecordWriteBackFieldRule, String> {
}
