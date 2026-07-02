package net.ximatai.muyun.spring.platform.impact;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface RecordImpactRelationDao extends BaseDao<RecordImpactRelation, String> {
}
