package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface EmployeeAccountDao extends BaseDao<EmployeeAccount, String> {
}
