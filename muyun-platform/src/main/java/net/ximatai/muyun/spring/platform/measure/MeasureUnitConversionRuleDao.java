package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface MeasureUnitConversionRuleDao extends BaseDao<MeasureUnitConversionRule, String> {
}
