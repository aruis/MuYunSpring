package net.ximatai.muyun.spring.boot.web;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.model.standard.StandardDataScopedEntity;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DataScopeWebTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void crudWebShouldDelegateReadEndpointsToDataScopeAbilityWhenAvailable() {
        DataScopedCrudService service = new DataScopedCrudService();
        DataScopedCrudController controller = new DataScopedCrudController(service);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            WebPageResponse<DataScopedRecord> page = controller.query(null);
            DataScopedRecord viewed = controller.view("record-1");

            assertThat(page.records()).extracting(DataScopedRecord::getTitle).containsExactly("Query");
            assertThat(viewed.getTitle()).isEqualTo("View record-1");
            assertThat(service.queryAction).isEqualTo(PlatformAction.QUERY);
            assertThat(service.viewAction).isEqualTo(PlatformAction.VIEW);
        }
    }

    @Test
    void treeWebShouldDelegateReadEndpointsToDataScopeAbilityWhenAvailable() {
        DataScopedTreeService service = new DataScopedTreeService();
        DataScopedTreeController controller = new DataScopedTreeController(service);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            WebListResponse<?> tree = controller.tree(true);
            WebListResponse<?> subtree = controller.tree("root-1", true, true);

            assertThat(tree.records()).hasSize(2);
            assertThat(subtree.records()).hasSize(2);
            assertThat(service.childrenActions).containsOnly(PlatformAction.TREE);
            assertThat(service.viewAction).isEqualTo(PlatformAction.TREE);
        }
    }

    @Getter
    @Setter
    private static class DataScopedRecord extends StandardDataScopedEntity {
        private String title;
    }

    @Getter
    @Setter
    private static final class DataScopedTreeRecord extends DataScopedRecord implements TreeCapable {
        private String parentId;
        private Integer sortOrder;
    }

    private static final class DataScopedCrudService extends AbstractAbilityService<DataScopedRecord>
            implements DataScopeAbility<DataScopedRecord> {
        private PlatformAction queryAction;
        private PlatformAction viewAction;

        private DataScopedCrudService() {
            super("demo.dataScopedCrud", DataScopedRecord.class, dao());
        }

        @Override
        public DataScopeCriteriaService getDataScopeCriteriaService() {
            return new AllowAllDataScopeCriteriaService();
        }

        @Override
        public PageResult<DataScopedRecord> pageQueryForAction(PlatformAction action,
                                                               Criteria criteria,
                                                               PageRequest pageRequest,
                                                               Sort... sorts) {
            queryAction = action;
            DataScopedRecord record = new DataScopedRecord();
            record.setTitle("Query");
            return PageResult.of(List.of(record), 1, pageRequest);
        }

        @Override
        public DataScopedRecord selectForAction(PlatformAction action, String id) {
            viewAction = action;
            DataScopedRecord record = new DataScopedRecord();
            record.setTitle("View " + id);
            return record;
        }
    }

    private static final class DataScopedTreeService extends AbstractAbilityService<DataScopedTreeRecord>
            implements TreeAbility<DataScopedTreeRecord>, DataScopeAbility<DataScopedTreeRecord> {
        private final java.util.ArrayList<PlatformAction> childrenActions = new java.util.ArrayList<>();
        private PlatformAction viewAction;

        private DataScopedTreeService() {
            super("demo.dataScopedTree", DataScopedTreeRecord.class, dao());
        }

        @Override
        public DataScopeCriteriaService getDataScopeCriteriaService() {
            return new AllowAllDataScopeCriteriaService();
        }

        @Override
        public DataScopedTreeRecord selectForAction(PlatformAction action, String id) {
            viewAction = action;
            return treeRecord(id, "Root", TreeAbility.ROOT_ID);
        }

        @Override
        public List<DataScopedTreeRecord> childrenForAction(PlatformAction action, String parentId) {
            childrenActions.add(action);
            if (TreeAbility.ROOT_ID.equals(parentId)) {
                return List.of(treeRecord("root-1", "Root", TreeAbility.ROOT_ID));
            }
            if ("root-1".equals(parentId)) {
                return List.of(treeRecord("child-1", "Child", parentId));
            }
            return List.of();
        }
    }

    private static final class DataScopedCrudController extends WebSupport<DataScopedCrudService>
            implements CrudWeb<DataScopedRecord, DataScopedCrudService> {
        private DataScopedCrudController(DataScopedCrudService service) {
            this.service = service;
        }
    }

    private static final class DataScopedTreeController extends WebSupport<DataScopedTreeService>
            implements TreeWeb<DataScopedTreeRecord, DataScopedTreeService> {
        private DataScopedTreeController(DataScopedTreeService service) {
            this.service = service;
        }
    }

    private static DataScopedTreeRecord treeRecord(String id, String title, String parentId) {
        DataScopedTreeRecord record = new DataScopedTreeRecord();
        record.setId(id);
        record.setTitle(title);
        record.setParentId(parentId);
        return record;
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.ximatai.muyun.spring.common.model.contract.EntityContract> BaseDao<T, String> dao() {
        return mock(BaseDao.class);
    }
}
