package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface EmployeePositionDao extends BaseDao<EmployeePosition, String> {
}
