package net.ximatai.muyun.spring.iam.position;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.action.BusinessExceptions;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PositionCategoryService extends TenantStandardBusinessService<PositionCategory> implements
        SoftDeleteAbility<PositionCategory>,
        EnableAbility<PositionCategory>,
        TreeAbility<PositionCategory>,
        ReferenceAbility<PositionCategory> {
    public static final String MODULE_ALIAS = "iam.position_category";
    private final PositionCategoryDao positionCategoryDao;
    private final PositionDao positionDao;

    public PositionCategoryService(PositionCategoryDao positionCategoryDao,
                                   ActiveTenantVerifier activeTenantVerifier,
                                   PositionDao positionDao) {
        super(MODULE_ALIAS, PositionCategory.class, positionCategoryDao, activeTenantVerifier);
        this.positionCategoryDao = positionCategoryDao;
        this.positionDao = positionDao;
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

    @Override
    public void beforeDelete(String id) {
        String tenantId = requireActiveTenantMutationContext();
        String categoryId = Preconditions.requireText(id, "positionCategoryId");
        long childCategories = positionCategoryDao.count(Criteria.of()
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, categoryId)
                .eq(StandardEntitySchema.TENANT_ID_FIELD, tenantId)
                .eq(StandardEntitySchema.DELETED_FIELD, Boolean.FALSE));
        if (childCategories > 0) {
            throw BusinessExceptions.warning("iam.position-category.delete-has-children",
                    "该岗位分类下仍有子分类，不能删除");
        }
        long referencedPositions = positionDao.count(Criteria.of()
                .eq("categoryId", categoryId)
                .eq(StandardEntitySchema.TENANT_ID_FIELD, tenantId)
                .eq(StandardEntitySchema.DELETED_FIELD, Boolean.FALSE));
        if (referencedPositions > 0) {
            throw BusinessExceptions.warning("iam.position-category.delete-referenced",
                    "该岗位分类已被岗位引用，不能删除",
                    Map.of("referenceCount", referencedPositions));
        }
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
