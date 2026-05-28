package net.ximatai.muyun.spring.platform.application;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaGroup;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.BaseDao;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationServiceContractTest {
    @Test
    void shouldRequireExplicitApplicationAliasBeforeIdGeneration() {
        ApplicationService service = new ApplicationService(new ApplicationMemoryDao());
        Application application = new Application();
        application.setTitle("CRM");

        assertThatThrownBy(() -> service.insert(application))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("applicationAlias");
    }

    @Test
    void shouldUseAliasAsApplicationIdAndFillAbilityDefaults() {
        ApplicationService service = new ApplicationService(new ApplicationMemoryDao());
        Application application = new Application();
        application.setAlias("crm");
        application.setTitle("CRM");

        String id = service.insert(application);

        assertThat(id).isEqualTo("crm");
        assertThat(application.getId()).isEqualTo("crm");
        assertThat(application.getEnabled()).isTrue();
    }

    @Test
    void shouldRejectInvalidApplicationAlias() {
        ApplicationService service = new ApplicationService(new ApplicationMemoryDao());
        Application application = new Application();
        application.setAlias("crm-app");
        application.setTitle("CRM");

        assertThatThrownBy(() -> service.insert(application))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationAlias");
    }

    @Test
    void shouldReorderApplicationsInGlobalScope() {
        ApplicationMemoryDao dao = new ApplicationMemoryDao();
        ApplicationService service = new ApplicationService(dao);
        service.insert(application("crm"));
        service.insert(application("sales"));

        service.reorder(List.of("sales", "crm"));

        assertThat(service.sortedList(Criteria.of()))
                .extracting(Application::getAlias)
                .containsExactly("sales", "crm");
    }

    private Application application(String alias) {
        Application application = new Application();
        application.setAlias(alias);
        application.setTitle(alias);
        return application;
    }

    private static class ApplicationMemoryDao implements BaseDao<Application, String> {
        private final Map<String, Application> rows = new LinkedHashMap<>();

        @Override
        public boolean ensureTable() {
            return true;
        }

        @Override
        public String insert(Application entity) {
            rows.put(entity.getId(), entity);
            return entity.getId();
        }

        @Override
        public int updateById(Application entity) {
            rows.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public int updateByIdAndCondition(Application entity, Map<String, Object> conditions) {
            rows.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public int deleteById(String id) {
            return rows.remove(id) == null ? 0 : 1;
        }

        @Override
        public int deleteByIdAndCondition(String id, Map<String, Object> conditions) {
            return 1;
        }

        @Override
        public boolean existsById(String id) {
            return rows.containsKey(id);
        }

        @Override
        public Application findById(String id) {
            return rows.get(id);
        }

        @Override
        public List<Application> query(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            List<Application> filtered = rows.values().stream()
                    .filter(row -> matches(row, criteria))
                    .sorted(Comparator.comparing(Application::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                    .toList();
            int from = Math.min(pageRequest.getOffset(), filtered.size());
            int to = Math.min(from + pageRequest.getLimit(), filtered.size());
            return new ArrayList<>(filtered.subList(from, to));
        }

        @Override
        public PageResult<Application> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            return PageResult.of(List.of(), 0, pageRequest);
        }

        @Override
        public long count(Criteria criteria) {
            return rows.values().stream().filter(row -> matches(row, criteria)).count();
        }

        @Override
        public int upsert(Application entity) {
            rows.put(entity.getId(), entity);
            return 1;
        }

        private boolean matches(Application row, Criteria criteria) {
            if (criteria == null || criteria.isEmpty()) {
                return true;
            }
            return matchesGroup(row, criteria.getRoot());
        }

        private boolean matchesGroup(Application row, CriteriaGroup group) {
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

        private boolean matchesNode(Application row, Object node) {
            if (node instanceof CriteriaClause clause) {
                return matchesClause(row, clause);
            }
            if (node instanceof CriteriaGroup group) {
                return matchesGroup(row, group);
            }
            return true;
        }

        private boolean matchesClause(Application row, CriteriaClause clause) {
            if (clause.getOperator() != CriteriaOperator.EQ) {
                return true;
            }
            Object expected = clause.getValues().getFirst();
            Object actual = switch (clause.getField()) {
                case "id" -> row.getId();
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
