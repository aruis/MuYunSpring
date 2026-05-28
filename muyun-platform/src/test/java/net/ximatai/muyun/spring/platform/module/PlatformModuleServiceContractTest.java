package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaGroup;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.TreeAbility;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformModuleServiceContractTest {
    @Test
    void shouldRequireModuleAliasBeforeIdGeneration() {
        PlatformModuleService service = new PlatformModuleService(new ModuleMemoryDao());
        PlatformModule module = new PlatformModule();
        module.setApplicationAlias("crm");
        module.setTitle("Customer");

        assertThatThrownBy(() -> service.insert(module))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("moduleAlias");
    }

    @Test
    void shouldUseAliasAsModuleIdAndFillTreeDefaults() {
        PlatformModuleService service = new PlatformModuleService(new ModuleMemoryDao());
        PlatformModule module = module("crm.customer", "crm");

        String id = service.insert(module);

        assertThat(id).isEqualTo("crm.customer");
        assertThat(module.getId()).isEqualTo("crm.customer");
        assertThat(module.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
        assertThat(module.getEnabled()).isTrue();
        assertThat(module.getModuleKind()).isEqualTo(ModuleKind.STATIC);
    }

    @Test
    void shouldRejectModuleAliasOutsideApplication() {
        PlatformModuleService service = new PlatformModuleService(new ModuleMemoryDao());
        PlatformModule module = module("sales.customer", "crm");

        assertThatThrownBy(() -> service.insert(module))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moduleAlias");
    }

    @Test
    void shouldSupportModuleTreeWithinSameApplication() {
        ModuleMemoryDao dao = new ModuleMemoryDao();
        PlatformModuleService service = new PlatformModuleService(dao);
        service.insert(module("crm.customer", "crm"));
        PlatformModule child = module("crm.customer.profile", "crm");
        child.setParentId("crm.customer");

        service.insert(child);

        assertThat(service.children("crm.customer"))
                .extracting(PlatformModule::getAlias)
                .containsExactly("crm.customer.profile");
    }

    @Test
    void shouldResolveRootModulesByApplication() {
        ModuleMemoryDao dao = new ModuleMemoryDao();
        PlatformModuleService service = new PlatformModuleService(dao);
        service.insert(module("crm.customer", "crm"));
        service.insert(module("sales.contract", "sales"));

        assertThat(service.rootModules("crm"))
                .extracting(PlatformModule::getAlias)
                .containsExactly("crm.customer");
    }

    @Test
    void shouldRejectUnscopedRootChildrenLookup() {
        PlatformModuleService service = new PlatformModuleService(new ModuleMemoryDao());

        assertThatThrownBy(() -> service.children(TreeAbility.ROOT_ID))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("rootModules");
    }

    @Test
    void shouldResolveChildrenByApplication() {
        ModuleMemoryDao dao = new ModuleMemoryDao();
        PlatformModuleService service = new PlatformModuleService(dao);
        service.insert(module("crm.customer", "crm"));
        PlatformModule crmChild = module("crm.customer.profile", "crm");
        crmChild.setParentId("crm.customer");
        service.insert(crmChild);

        assertThat(service.children("sales", "crm.customer")).isEmpty();
        assertThat(service.children("crm", "crm.customer"))
                .extracting(PlatformModule::getAlias)
                .containsExactly("crm.customer.profile");
    }

    @Test
    void shouldRejectModuleTreeAcrossApplications() {
        ModuleMemoryDao dao = new ModuleMemoryDao();
        PlatformModuleService service = new PlatformModuleService(dao);
        service.insert(module("crm.customer", "crm"));
        PlatformModule child = module("sales.contract", "sales");
        child.setParentId("crm.customer");

        assertThatThrownBy(() -> service.insert(child))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("same application");
    }

    @Test
    void shouldReorderModulesWithinSameApplicationAndParent() {
        ModuleMemoryDao dao = new ModuleMemoryDao();
        PlatformModuleService service = new PlatformModuleService(dao);
        service.insert(module("crm.customer", "crm"));
        service.insert(module("crm.contract", "crm"));

        service.reorder(List.of("crm.contract", "crm.customer"));

        assertThat(service.rootModules("crm"))
                .extracting(PlatformModule::getAlias)
                .containsExactly("crm.contract", "crm.customer");
    }

    @Test
    void shouldRejectReorderAcrossApplicationOrParent() {
        ModuleMemoryDao dao = new ModuleMemoryDao();
        PlatformModuleService service = new PlatformModuleService(dao);
        service.insert(module("crm.customer", "crm"));
        service.insert(module("sales.contract", "sales"));
        PlatformModule child = module("crm.customer.profile", "crm");
        child.setParentId("crm.customer");
        service.insert(child);

        assertThatThrownBy(() -> service.reorder(List.of("crm.customer", "sales.contract")))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("same application");
        assertThatThrownBy(() -> service.reorder(List.of("crm.customer", "crm.customer.profile")))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("same parent");
    }

    private PlatformModule module(String alias, String applicationAlias) {
        PlatformModule module = new PlatformModule();
        module.setAlias(alias);
        module.setApplicationAlias(applicationAlias);
        module.setTitle(alias);
        return module;
    }

    private static class ModuleMemoryDao implements BaseDao<PlatformModule, String> {
        private final Map<String, PlatformModule> rows = new LinkedHashMap<>();

        @Override
        public boolean ensureTable() {
            return true;
        }

        @Override
        public String insert(PlatformModule entity) {
            rows.put(entity.getId(), entity);
            return entity.getId();
        }

        @Override
        public int updateById(PlatformModule entity) {
            rows.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public int updateByIdAndCondition(PlatformModule entity, Map<String, Object> conditions) {
            rows.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public int deleteById(String id) {
            return rows.remove(id) == null ? 0 : 1;
        }

        @Override
        public int deleteByIdAndCondition(String id, Map<String, Object> conditions) {
            return deleteById(id);
        }

        @Override
        public boolean existsById(String id) {
            return rows.containsKey(id);
        }

        @Override
        public PlatformModule findById(String id) {
            return rows.get(id);
        }

        @Override
        public List<PlatformModule> query(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            List<PlatformModule> filtered = rows.values().stream()
                    .filter(row -> matches(row, criteria))
                    .sorted(Comparator.comparing(PlatformModule::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                    .toList();
            int from = Math.min(pageRequest.getOffset(), filtered.size());
            int to = Math.min(from + pageRequest.getLimit(), filtered.size());
            return new ArrayList<>(filtered.subList(from, to));
        }

        @Override
        public PageResult<PlatformModule> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            List<PlatformModule> records = query(criteria, pageRequest, sorts);
            return PageResult.of(records, records.size(), pageRequest);
        }

        @Override
        public long count(Criteria criteria) {
            return rows.values().stream().filter(row -> matches(row, criteria)).count();
        }

        @Override
        public int upsert(PlatformModule entity) {
            rows.put(entity.getId(), entity);
            return 1;
        }

        private boolean matches(PlatformModule row, Criteria criteria) {
            if (criteria == null || criteria.isEmpty()) {
                return true;
            }
            return matchesGroup(row, criteria.getRoot());
        }

        private boolean matchesGroup(PlatformModule row, CriteriaGroup group) {
            Boolean matched = null;
            for (CriteriaGroup.Entry entry : group.getEntries()) {
                boolean entryMatched = matchesNode(row, entry.getNode());
                if (matched == null) {
                    matched = entryMatched;
                } else if (isOrJoin(entry)) {
                    matched = matched || entryMatched;
                } else {
                    matched = matched && entryMatched;
                }
            }
            return matched == null || matched;
        }

        private boolean matchesNode(PlatformModule row, Object node) {
            if (node instanceof CriteriaClause clause) {
                return matchesClause(row, clause);
            }
            if (node instanceof CriteriaGroup group) {
                return matchesGroup(row, group);
            }
            return true;
        }

        private boolean matchesClause(PlatformModule row, CriteriaClause clause) {
            if (clause.getOperator() != CriteriaOperator.EQ) {
                return true;
            }
            Object expected = clause.getValues().getFirst();
            Object actual = switch (clause.getField()) {
                case "id" -> row.getId();
                case "applicationAlias" -> row.getApplicationAlias();
                case "parentId" -> row.getParentId();
                case "deleted" -> row.getDeleted();
                default -> null;
            };
            return expected == null ? actual == null : expected.equals(actual);
        }

        private boolean isOrJoin(CriteriaGroup.Entry entry) {
            try {
                Method method = entry.getClass().getMethod("getJoin");
                return "OR".equals(String.valueOf(method.invoke(entry)));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read criteria join", e);
            }
        }
    }
}
