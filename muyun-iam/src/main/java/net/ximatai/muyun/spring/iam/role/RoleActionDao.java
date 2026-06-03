package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface RoleActionDao extends BaseDao<RoleAction, String> {
}
