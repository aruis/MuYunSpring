package net.ximatai.muyun.spring.iam.department;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface DepartmentDao extends BaseDao<Department, String> {
}
