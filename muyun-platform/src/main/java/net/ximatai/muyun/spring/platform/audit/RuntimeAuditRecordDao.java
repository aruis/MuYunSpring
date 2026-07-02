package net.ximatai.muyun.spring.platform.audit;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface RuntimeAuditRecordDao extends BaseDao<RuntimeAuditRecord, String> {
}
