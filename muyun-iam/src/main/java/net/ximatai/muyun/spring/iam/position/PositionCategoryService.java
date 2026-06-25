package net.ximatai.muyun.spring.iam.position;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PositionCategoryService extends TenantStandardBusinessService<PositionCategory> implements
        SoftDeleteAbility<PositionCategory>,
        EnableAbility<PositionCategory>,
        TreeAbility<PositionCategory>,
        ReferenceAbility<PositionCategory> {
    public static final String MODULE_ALIAS = "iam.position_category";

    @Autowired
    public PositionCategoryService(PositionCategoryDao positionCategoryDao,
                                   ActiveTenantVerifier activeTenantVerifier) {
        super(MODULE_ALIAS, PositionCategory.class, positionCategoryDao, activeTenantVerifier);
    }

    @Override
    public void normalizeBeforeMutation(PositionCategory category) {
        category.setCode(Preconditions.requireText(category.getCode(), "positionCategoryCode"));
        category.setTitle(Preconditions.requireText(category.getTitle(), "positionCategoryTitle"));
        category.setDescription(normalizeBlank(category.getDescription()));
    }

    @Override
    protected void validateBeforeSave(PositionCategory category) {
        rejectDuplicate(category, Criteria.of().eq("code", category.getCode()),
                "positionCategoryCode must be unique within tenant: " + category.getCode());
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
