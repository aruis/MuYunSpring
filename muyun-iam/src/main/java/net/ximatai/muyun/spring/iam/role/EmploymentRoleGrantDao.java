package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface EmploymentRoleGrantDao extends BaseDao<EmploymentRoleGrant, String> {
}
