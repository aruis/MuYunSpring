package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface WorkflowDelegationDao extends BaseDao<WorkflowDelegation, String> {
}
