package net.ximatai.muyun.spring.demo.school.hobby;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import org.springframework.stereotype.Service;

/**
 * 爱好分类树的标准 Service：{@link TreeAbility} 提供层级与同级排序，{@link EnableAbility} 控制候选项可用性；
 * {@link ReferenceAbility} 将启用的分类节点交付为学生多选引用的统一候选项。
 */
@Service
public class HobbyService extends AbstractAbilityService<Hobby> implements
        SoftDeleteAbility<Hobby>,
        EnableAbility<Hobby>,
        TreeAbility<Hobby>,
        CacheAbility<Hobby>,
        ReferenceAbility<Hobby> {
    public static final String MODULE_ALIAS = "education.hobby";

    public HobbyService(HobbyDao dao) {
        super(MODULE_ALIAS, Hobby.class, dao);
    }
}
