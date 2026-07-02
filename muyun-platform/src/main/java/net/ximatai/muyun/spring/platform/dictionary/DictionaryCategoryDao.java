package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface DictionaryCategoryDao extends BaseDao<DictionaryCategory, String> {
}
