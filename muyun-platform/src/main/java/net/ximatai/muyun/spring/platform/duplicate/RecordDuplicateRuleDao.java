package net.ximatai.muyun.spring.platform.duplicate;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface RecordDuplicateRuleDao extends BaseDao<RecordDuplicateRule, String> {
}
