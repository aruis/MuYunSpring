package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface CodeRuleSegmentDao extends BaseDao<CodeRuleSegment, String> {
}
