package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ScopedTreeWebTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldReadTreeChildrenInsideResolvedScope() {
        ScopedTreeService service = new ScopedTreeService();
        ScopedTreeController controller = new ScopedTreeController(service);
        MockHttpServletRequest request = new MockHttpServletRequest();

        WebListResponse<?> response;
        try (TenantContext.Scope ignored = TenantContext.use("tenant_b")) {
            response = controller.tree(request, true);
        }

        assertThat(response.records()).hasSize(1);
        assertThat(service.childrenScope).isNotNull();
        assertThat(service.childrenParentId).isEqualTo(TreeAbility.ROOT_ID);
    }

    @Test
    void shouldMoveTreeInsideResolvedTenantScopeAndRestorePreviousContext() {
        ScopedTreeService service = new ScopedTreeService();
        ScopedTreeController controller = new ScopedTreeController(service);
        MockHttpServletRequest request = new MockHttpServletRequest();

        try (TenantContext.Scope ignored = TenantContext.system("scoped tree maintenance")) {
            int response = controller.sort(request,
                    "moving", new TreeSortWebRequest("previous", null, TreeAbility.ROOT_ID));

            assertThat(response).isEqualTo(1);
            assertThat(service.moveTenantId).isEqualTo("tenant_b");
            assertThat(service.moveScope).isNotNull();
            assertThat(TenantContext.isSystem()).isTrue();
        }
    }

    private static final class ScopedTreeController extends WebSupport<ScopedTreeService>
            implements ScopedTreeWeb<ScopedTreeRecord, ScopedTreeService> {
        private ScopedTreeController(ScopedTreeService service) {
            this.service = service;
        }

        @Override
        public TreeScope treeScope(HttpServletRequest request) {
            return TreeScope.tenant(Criteria.of().eq("tenantId", "tenant_b"), "tenant_b");
        }
    }

    private static final class ScopedTreeService extends AbstractAbilityService<ScopedTreeRecord>
            implements TreeAbility<ScopedTreeRecord> {
        private Criteria childrenScope;
        private String childrenParentId;
        private Criteria moveScope;
        private String moveTenantId;

        private ScopedTreeService() {
            super("test.scopedTree", ScopedTreeRecord.class, dao());
        }

        @Override
        public List<ScopedTreeRecord> children(Criteria scopeCriteria, String parentId) {
            childrenScope = scopeCriteria;
            childrenParentId = parentId;
            return List.of(record("root-1", TreeAbility.ROOT_ID));
        }

        @Override
        public void moveInTree(Criteria scopeCriteria, String id, String previousId, String nextId, String parentId) {
            moveScope = scopeCriteria;
            moveTenantId = TenantContext.currentTenantId().orElse(null);
        }
    }

    @Getter
    @Setter
    private static final class ScopedTreeRecord extends StandardEntity implements TreeCapable {
        private String parentId;
        private Integer sortOrder;
    }

    private static ScopedTreeRecord record(String id, String parentId) {
        ScopedTreeRecord record = new ScopedTreeRecord();
        record.setId(id);
        record.setParentId(parentId);
        return record;
    }

    @SuppressWarnings("unchecked")
    private static BaseDao<ScopedTreeRecord, String> dao() {
        return mock(BaseDao.class);
    }
}
