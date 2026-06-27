package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.PlatformManagedProtectionAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class PlatformModuleService extends AbstractAbilityService<PlatformModule> implements
        SoftDeleteAbility<PlatformModule>,
        EnableAbility<PlatformModule>,
        TreeAbility<PlatformModule>,
        PlatformManagedProtectionAbility<PlatformModule>,
        QueryAbility<PlatformModule> {

    public static final String MODULE_ALIAS = "platform.module";

    public PlatformModuleService(BaseDao<PlatformModule, String> moduleDao) {
        super(MODULE_ALIAS, PlatformModule.class, moduleDao);
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("parentId", QueryOperator.EQ, QueryOperator.IN).withTitle("父模块"))
                .field(QueryField.of("applicationAlias", QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("所属应用"))
                .field(QueryField.of("moduleKind", QueryOperator.EQ).withTitle("模块类型"))
                .field(QueryField.of("systemManaged", QueryValueType.BOOLEAN, QueryOperator.EQ)
                        .withTitle("系统管理"))
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("模块名称").withQuickSearch().withSortable())
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("启用状态"))
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                        .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("创建时间").withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("更新时间").withSortable())
                .defaultSort(Sort.asc("sortOrder"))
                .build();
    }

    @Override
    public void beforePrepareInsert(PlatformModule module) {
        normalizeAndValidate(module);
    }

    @Override
    public void beforeInsert(PlatformModule module) {
        normalizeAndValidate(module);
    }

    @Override
    public void beforeUpdate(PlatformModule module) {
        normalizeAndValidate(module);
    }

    @Override
    public Criteria sortScope(PlatformModule module) {
        return scopedTreeCriteria(module, "applicationAlias");
    }

    @Override
    public void validateSortScope(PlatformModule left, PlatformModule right) {
        validateTreeSortScopeByFields(left, right,
                "Module sort can only move records within the same application", "applicationAlias");
    }

    @Override
    public List<PlatformModule> children(String parentId) {
        if (TreeAbility.ROOT_ID.equals(parentId)) {
            rejectRootChildrenLookup("rootModules(applicationAlias)");
        }
        return TreeAbility.super.children(parentId);
    }

    public List<PlatformModule> rootModules(String applicationAlias) {
        return children(applicationAlias, TreeAbility.ROOT_ID);
    }

    public List<PlatformModule> children(String applicationAlias, String parentId) {
        return TreeAbility.super.children(applicationScope(PlatformNameRules.requireApplicationAlias(applicationAlias)), parentId);
    }

    public PlatformModule resolveVisibleModule(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        if (TenantContext.currentTenantId().isPresent()) {
            PlatformModule scoped = select(validAlias);
            if (scoped != null) {
                return scoped;
            }
        }
        return selectGlobalModule(validAlias);
    }

    public List<PlatformModule> listSystemManagedStaticModules() {
        try (TenantContext.Scope ignored = TenantContext.system("select system managed static modules")) {
            return list(Criteria.of()
                    .eq("moduleKind", ModuleKind.STATIC)
                    .eq("systemManaged", Boolean.TRUE)
                    .isNull(StandardEntitySchema.TENANT_ID_FIELD),
                    new PageRequest(0, Integer.MAX_VALUE));
        }
    }

    private PlatformModule selectGlobalModule(String moduleAlias) {
        try (TenantContext.Scope ignored = TenantContext.system("select global platform module")) {
            return getDao().query(activeCriteria(Criteria.of()
                            .eq("id", moduleAlias)),
                    new PageRequest(0, 1))
                    .stream()
                    .filter(module -> module.getTenantId() == null || module.getTenantId().isBlank())
                    .findFirst()
                    .orElse(null);
        }
    }

    private void normalizeAndValidate(PlatformModule module) {
        String applicationAlias = requireApplicationAlias(module.getApplicationAlias());
        String moduleAlias = requireModuleAlias(module.getAlias(), applicationAlias);
        module.setApplicationAlias(applicationAlias);
        module.setAlias(moduleAlias);
        if (module.getModuleKind() == null) {
            module.setModuleKind(ModuleKind.STATIC);
        }
        normalizeEntry(module);
        validateParentApplication(module);
    }

    private void normalizeEntry(PlatformModule module) {
        if (module.getEntryType() == null) {
            module.setEntryType(ModuleEntryType.MODULE);
        }
        switch (module.getEntryType()) {
            case MODULE -> {
                module.setEntryRoute(null);
                module.setEntryExternalUrl(null);
            }
            case ROUTE -> {
                module.setEntryRoute(normalizeInternalRoute(module.getEntryRoute()));
                module.setEntryExternalUrl(null);
            }
            case LINK -> {
                module.setEntryRoute(null);
                module.setEntryExternalUrl(requireText(module.getEntryExternalUrl(), "LINK module entry requires externalUrl").trim());
            }
        }
    }

    private String normalizeInternalRoute(String route) {
        String normalized = requireText(route, "ROUTE module entry requires route").trim();
        if (!normalized.startsWith("/") || normalized.startsWith("//") || normalized.contains("://")) {
            throw new PlatformException("ROUTE module entry route must be an internal path: " + normalized);
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }

    private String requireApplicationAlias(String applicationAlias) {
        return PlatformNameRules.requireApplicationAlias(applicationAlias);
    }

    private String requireModuleAlias(String moduleAlias, String applicationAlias) {
        return PlatformNameRules.requireModuleAliasInApplication(moduleAlias, applicationAlias);
    }

    private void validateParentApplication(PlatformModule module) {
        validateTreePlacementInScope(module, applicationScope(module.getApplicationAlias()),
                "Module parent must belong to the same application");
    }

    private Criteria applicationScope(String applicationAlias) {
        return Criteria.of().eq("applicationAlias", applicationAlias);
    }
}
