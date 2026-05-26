package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.IMetaDataLoader;
import net.ximatai.muyun.database.core.metadata.DBColumn;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.metadata.DBIndex;
import net.ximatai.muyun.database.core.metadata.DBSchema;
import net.ximatai.muyun.database.core.metadata.DBTable;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.spring.boot.sql.MuYunRepositoryFactory;
import net.ximatai.muyun.spring.ability.TreeAbility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationRepositoryContractTest {
    private static final String SCHEMA = "public";
    private static final String TABLE = "iam_organization";

    @Test
    void abilityServiceShouldUseMuYunRepositoryProxyForInsert() {
        IDatabaseOperations<Object> operations = mockedOperations();
        when(operations.insertItem(eq(SCHEMA), eq(TABLE), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));

        OrganizationService service = new OrganizationService(repository(operations));
        Organization organization = new Organization();
        organization.setCode("HQ");
        organization.setName("Headquarters");

        String id = service.insert(organization);

        assertThat(id).hasSize(32);
        assertThat(organization.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
        assertThat(organization.getEnabled()).isTrue();

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), body.capture());
        assertThat(body.getValue())
                .containsEntry("id", id)
                .containsEntry("code", "HQ")
                .containsEntry("name", "Headquarters")
                .containsEntry("parent_id", TreeAbility.ROOT_ID)
                .containsEntry("enabled", Boolean.TRUE)
                .containsEntry("deleted", Boolean.FALSE)
                .containsEntry("version", 0);
    }

    @Test
    void repositoryShouldResolveStaticModelForEnsureTable() {
        IDatabaseOperations<Object> operations = mockedOperationsWithExistingOrganizationTable();

        assertThat(repository(operations).ensureTable()).isFalse();

        verify(operations).execute(contains("comment on table \"public\".\"iam_organization\""));
        verify(operations, never()).insertItem(anyString(), anyString(), anyMap());
    }

    @Test
    void abilityQueriesShouldCompileThroughMuYunRepositoryProxy() {
        IDatabaseOperations<Object> operations = mockedOperations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(row("org-1", "Headquarters", 10)));

        OrganizationService service = new OrganizationService(repository(operations));

        assertThat(service.pageQuery(Criteria.of().eq("parentId", TreeAbility.ROOT_ID), PageRequest.of(1, 10)).getRecords())
                .extracting(Organization::getName)
                .containsExactly("Headquarters");
        assertThat(service.children(TreeAbility.ROOT_ID))
                .extracting(Organization::getId)
                .containsExactly("org-1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, times(2)).query(sql.capture(), anyMap());

        assertThat(sql.getAllValues().get(0))
                .contains("\"parent_id\" =")
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL")
                .contains("OR");
        assertThat(sql.getAllValues().get(1))
                .contains("ORDER BY \"sort_order\" ASC");
    }

    private OrganizationDao repository(IDatabaseOperations<Object> operations) {
        return new MuYunRepositoryFactory(operations, new MockEnvironment()).create(OrganizationDao.class);
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> mockedOperations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn(SCHEMA);
        return operations;
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> mockedOperationsWithExistingOrganizationTable() {
        IMetaDataLoader loader = mock(IMetaDataLoader.class);
        DBInfo dbInfo = new DBInfo("POSTGRESQL").setName("muyun_test");
        DBSchema schema = new DBSchema(SCHEMA);
        schema.addTable(new DBTable(loader).setName(TABLE).setSchema(SCHEMA));
        dbInfo.addSchema(schema);

        when(loader.getDBInfo()).thenReturn(dbInfo);
        when(loader.getColumnMap(SCHEMA, TABLE)).thenReturn(organizationColumns());
        when(loader.getIndexList(SCHEMA, TABLE)).thenReturn(List.of(index("iam_organization_code_uindex", true, "code")));

        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getMetaDataLoader()).thenReturn(loader);
        when(operations.getDBInfo()).thenReturn(dbInfo);
        when(operations.getDefaultSchemaName()).thenReturn(SCHEMA);
        return operations;
    }

    private Map<String, Object> row(String id, String name, int sortOrder) {
        return Map.of(
                "id", id,
                "code", id.toUpperCase(),
                "name", name,
                "parent_id", TreeAbility.ROOT_ID,
                "sort_order", sortOrder,
                "enabled", Boolean.TRUE,
                "deleted", Boolean.FALSE,
                "version", 0
        );
    }

    private Map<String, DBColumn> organizationColumns() {
        Map<String, DBColumn> columns = new LinkedHashMap<>();
        columns.put("id", column("id", "VARCHAR", 32, false, true, "ID"));
        columns.put("version", column("version", "INT", null, true, false, "Optimistic lock version"));
        columns.put("deleted", column("deleted", "BOOLEAN", null, true, false, "Soft delete flag"));
        columns.put("created_by", column("created_by", "VARCHAR", 64, true, false, "Created by"));
        columns.put("created_at", column("created_at", "TIMESTAMP", null, true, false, "Created at"));
        columns.put("updated_by", column("updated_by", "VARCHAR", 64, true, false, "Updated by"));
        columns.put("updated_at", column("updated_at", "TIMESTAMP", null, true, false, "Updated at"));
        columns.put("parent_id", column("parent_id", "VARCHAR", 32, true, false, "Parent organization ID"));
        columns.put("code", column("code", "VARCHAR", 64, false, false, "Organization code"));
        columns.put("name", column("name", "VARCHAR", 128, false, false, "Organization name"));
        columns.put("sort_order", column("sort_order", "INT", null, true, false, "Sort order"));
        columns.put("enabled", column("enabled", "BOOLEAN", null, true, false, "Enabled flag"));
        return columns;
    }

    private DBColumn column(String name, String type, Integer length, boolean nullable, boolean primaryKey, String description) {
        DBColumn column = new DBColumn();
        column.setName(name);
        column.setType(type);
        if (length != null) {
            column.setLength(length);
        }
        column.setNullable(nullable);
        column.setPrimaryKey(primaryKey);
        column.setDescription(description);
        return column;
    }

    private DBIndex index(String name, boolean unique, String column) {
        return new DBIndex().setName(name).setUnique(unique).addColumn(column);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
