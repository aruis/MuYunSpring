package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface ModuleMetadataRelationDao extends BaseDao<ModuleMetadataRelation, String> {
}
