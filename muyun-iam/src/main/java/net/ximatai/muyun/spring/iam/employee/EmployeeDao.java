package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface EmployeeDao extends BaseDao<Employee, String> {
}
