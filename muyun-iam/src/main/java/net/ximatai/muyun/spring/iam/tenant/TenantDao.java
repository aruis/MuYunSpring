package net.ximatai.muyun.spring.iam.tenant;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface TenantDao extends BaseDao<Tenant, String> {
}
