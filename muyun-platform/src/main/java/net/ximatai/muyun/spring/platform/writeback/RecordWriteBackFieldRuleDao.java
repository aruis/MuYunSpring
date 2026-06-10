package net.ximatai.muyun.spring.platform.writeback;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;

@MuYunRepository
public interface RecordWriteBackFieldRuleDao extends BaseDao<RecordWriteBackFieldRule, String> {
}
