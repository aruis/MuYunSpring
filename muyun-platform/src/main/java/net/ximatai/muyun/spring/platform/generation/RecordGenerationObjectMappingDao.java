package net.ximatai.muyun.spring.platform.generation;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface RecordGenerationObjectMappingDao extends BaseDao<RecordGenerationObjectMapping, String> {
}
