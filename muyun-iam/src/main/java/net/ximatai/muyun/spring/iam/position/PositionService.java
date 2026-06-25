package net.ximatai.muyun.spring.iam.position;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PositionService extends TenantStandardBusinessService<Position> implements
        SoftDeleteAbility<Position>,
        EnableAbility<Position>,
        SortAbility<Position>,
        ReferenceAbility<Position> {
    public static final String MODULE_ALIAS = "iam.position";

    private final PositionCategoryService positionCategoryService;

    @Autowired
    public PositionService(PositionDao positionDao,
                           ActiveTenantVerifier activeTenantVerifier,
                           PositionCategoryService positionCategoryService) {
        super(MODULE_ALIAS, Position.class, positionDao, activeTenantVerifier);
        this.positionCategoryService = positionCategoryService;
    }

    @Override
    public void normalizeBeforeMutation(Position position) {
        position.setCategoryId(normalizeBlank(position.getCategoryId()));
        position.setCode(Preconditions.requireText(position.getCode(), "positionCode"));
        position.setTitle(Preconditions.requireText(position.getTitle(), "positionTitle"));
        position.setDescription(normalizeBlank(position.getDescription()));
    }

    @Override
    protected void validateBeforeSave(Position position) {
        requireActiveCategory(position.getCategoryId());
        rejectDuplicate(position, Criteria.of().eq("code", position.getCode()),
                "positionCode must be unique within tenant: " + position.getCode());
    }

    @Override
    public Criteria sortScope(Position position) {
        return categoryScope(position.getCategoryId());
    }

    @Override
    public void validateSortScope(Position left, Position right) {
        validateSortScopeByFields(left, right,
                "Position sort can only move records within the same category", "categoryId");
    }

    private void requireActiveCategory(String categoryId) {
        if (categoryId == null) {
            return;
        }
        positionCategoryService.requireEnabled(categoryId,
                "position category is not active: " + categoryId);
    }

    private Criteria categoryScope(String categoryId) {
        Criteria criteria = Criteria.of();
        if (categoryId == null) {
            criteria.isNull("categoryId");
            return criteria;
        }
        return criteria.eq("categoryId", categoryId);
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
