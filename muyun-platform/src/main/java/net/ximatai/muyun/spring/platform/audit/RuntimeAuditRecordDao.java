package net.ximatai.muyun.spring.platform.audit;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface RuntimeAuditRecordDao extends BaseDao<RuntimeAuditRecord, String> {
}
