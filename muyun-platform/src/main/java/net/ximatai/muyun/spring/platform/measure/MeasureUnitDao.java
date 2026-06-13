package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface MeasureUnitDao extends BaseDao<MeasureUnit, String> {
}
