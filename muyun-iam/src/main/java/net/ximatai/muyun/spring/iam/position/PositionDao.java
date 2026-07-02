package net.ximatai.muyun.spring.iam.position;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface PositionDao extends BaseDao<Position, String> {
}
