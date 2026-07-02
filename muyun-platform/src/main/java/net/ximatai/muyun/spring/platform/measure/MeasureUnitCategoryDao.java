package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface MeasureUnitCategoryDao extends BaseDao<MeasureUnitCategory, String> {
}
