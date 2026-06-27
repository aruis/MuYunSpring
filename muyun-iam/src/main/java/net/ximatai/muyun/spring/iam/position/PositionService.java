package net.ximatai.muyun.spring.iam.position;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.employee.EmployeePositionDao;
import org.springframework.stereotype.Service;

@Service
public class PositionService extends TenantStandardBusinessService<Position> implements
        SoftDeleteAbility<Position>,
        EnableAbility<Position>,
        SortAbility<Position>,
        ReferenceAbility<Position>,
        QueryAbility<Position> {
    public static final String MODULE_ALIAS = "iam.position";

    private final PositionCategoryService positionCategoryService;
    private final EmployeePositionDao employeePositionDao;

    public PositionService(PositionDao positionDao,
                           ActiveTenantVerifier activeTenantVerifier,
                           PositionCategoryService positionCategoryService,
                           EmployeePositionDao employeePositionDao) {
        super(MODULE_ALIAS, Position.class, positionDao, activeTenantVerifier);
        this.positionCategoryService = positionCategoryService;
        this.employeePositionDao = employeePositionDao;
    }

    @Override
    public void normalizeBeforeMutation(Position position) {
        position.setCategoryId(Preconditions.requireText(position.getCategoryId(), "positionCategoryId"));
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
        return Criteria.of().eq("categoryId", position.getCategoryId());
    }

    @Override
    public void validateSortScope(Position left, Position right) {
        validateSortScopeByFields(left, right,
                "Position sort can only move records within the same category", "categoryId");
    }

    @Override
    public void beforeDelete(String id) {
        String tenantId = requireActiveTenantMutationContext();
        String positionId = Preconditions.requireText(id, "positionId");
        long referencedEmployeePositions = employeePositionDao.count(Criteria.of()
                .eq("positionId", positionId)
                .eq(StandardEntitySchema.TENANT_ID_FIELD, tenantId)
                .eq(StandardEntitySchema.DELETED_FIELD, Boolean.FALSE));
        if (referencedEmployeePositions > 0) {
            throw new PlatformException("position is referenced by employee positions: " + id);
        }
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("categoryId", QueryOperator.EQ, QueryOperator.IN).withTitle("岗位分类"))
                .field(QueryField.of("code", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("岗位编码").withSortable())
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("岗位名称").withSortable())
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("启用状态"))
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                        .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("创建时间")
                        .withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("更新时间")
                        .withSortable())
                .defaultSort(Sort.asc("sortOrder"))
                .defaultSort(Sort.asc("title"))
                .build();
    }

    private void requireActiveCategory(String categoryId) {
        if (categoryId == null) {
            return;
        }
        positionCategoryService.requireEnabled(categoryId,
                "position category is not active: " + categoryId);
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
