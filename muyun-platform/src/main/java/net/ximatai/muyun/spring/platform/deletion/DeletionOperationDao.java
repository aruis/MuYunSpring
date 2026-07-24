package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface DeletionOperationDao extends BaseDao<DeletionOperation, String> {
}
