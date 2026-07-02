package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface AccountRoleGrantDao extends BaseDao<AccountRoleGrant, String> {
}
