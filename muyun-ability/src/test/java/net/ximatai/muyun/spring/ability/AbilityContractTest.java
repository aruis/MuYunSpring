package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbilityContractTest {
    @Test
    void crudAbilityShouldFillStandardFieldsAndSoftDelete() {
        DemoOrganizationService service = new DemoOrganizationService();
        DemoOrganization organization = new DemoOrganization("Headquarters", TreeAbility.ROOT_ID);

        String id = service.insert(organization);

        assertThat(id).hasSize(32);
        assertThat(organization.getVersion()).isZero();
        assertThat(organization.getDeleted()).isFalse();
        assertThat(organization.getCreatedAt()).isNotNull();

        assertThat(service.select(id)).isSameAs(organization);

        assertThat(service.delete(id)).isEqualTo(1);
        assertThat(organization.getVersion()).isEqualTo(1);
        assertThat(service.select(id)).isNull();
        assertThat(service.selectIgnoreSoftDelete(id)).isSameAs(organization);
    }

    @Test
    void crudAbilityShouldApplyTenantContextAcrossDefaultEntrypoints() {
        DemoCachedPlainRecordService service = new DemoCachedPlainRecordService();
        String id;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            id = service.insert(new DemoPlainRecord("Tenant A"));
            assertThat(service.rawDao().findById(id).getTenantId()).isEqualTo("tenant-a");
            assertThat(service.select(id)).isNotNull();
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            assertThat(service.select(id)).isNull();
            assertThat(service.selectIgnoreSoftDelete(id)).isNull();
            assertThat(service.count(Criteria.of())).isZero();
            DemoPlainRecord update = new DemoPlainRecord("Tenant B overwrite");
            update.setId(id);
            assertThat(service.update(update)).isZero();
            assertThat(service.delete(id)).isZero();
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(service.select(id).getTitle()).isEqualTo("Tenant A");
            assertThat(service.count(Criteria.of())).isEqualTo(1);
        }
    }

    @Test
    void crudAbilityShouldDeleteRecordAndBatchByStandardEntry() {
        DemoOrganizationService service = new DemoOrganizationService();
        DemoOrganization first = new DemoOrganization("First", TreeAbility.ROOT_ID);
        DemoOrganization second = new DemoOrganization("Second", TreeAbility.ROOT_ID);
        DemoOrganization third = new DemoOrganization("Third", TreeAbility.ROOT_ID);

        service.insert(first);
        String secondId = service.insert(second);
        String thirdId = service.insert(third);

        assertThat(service.delete(first)).isEqualTo(1);
        assertThat(service.deleteBatch(List.of(secondId, thirdId, "missing"))).isEqualTo(2);
        assertThat(service.select(first.getId())).isNull();
        assertThat(service.select(secondId)).isNull();
        assertThat(service.select(thirdId)).isNull();
    }

    @Test
    void softDeleteAbilityShouldUpdateActiveRecordsOnly() {
        DemoOrganizationService service = new DemoOrganizationService();
        String id = service.insert(new DemoOrganization("Active", TreeAbility.ROOT_ID));

        DemoOrganization maliciousSoftDelete = new DemoOrganization("Updated", TreeAbility.ROOT_ID);
        maliciousSoftDelete.setId(id);
        maliciousSoftDelete.setDeleted(Boolean.TRUE);

        assertThat(service.update(maliciousSoftDelete)).isEqualTo(1);
        assertThat(service.select(id).getTitle()).isEqualTo("Updated");
        assertThat(service.select(id).getDeleted()).isFalse();

        assertThat(service.delete(id)).isEqualTo(1);
        DemoOrganization resurrect = new DemoOrganization("Resurrect", TreeAbility.ROOT_ID);
        resurrect.setId(id);
        resurrect.setDeleted(Boolean.FALSE);

        assertThat(service.update(resurrect)).isZero();
        assertThat(service.select(id)).isNull();
        assertThat(service.selectIgnoreSoftDelete(id).getTitle()).isEqualTo("Updated");
        assertThat(service.selectIgnoreSoftDelete(id).getDeleted()).isTrue();
    }

    @Test
    void crudAbilityShouldStayNeutralWithoutSoftDeleteAbility() {
        DemoPlainRecordService service = new DemoPlainRecordService();
        DemoPlainRecord first = new DemoPlainRecord("First");
        DemoPlainRecord second = new DemoPlainRecord("Second");

        String firstId = service.insert(first);
        String secondId = service.insert(second);
        first.setDeleted(Boolean.TRUE);

        assertThat(service.select(firstId)).isSameAs(first);
        assertThat(service.pageQuery(Criteria.of(), PageRequest.of(1, 10)).getRecords())
                .containsExactly(first, second);
        assertThat(service.deleteBatch(List.of(firstId, secondId))).isEqualTo(2);
        assertThat(service.getDao().findById(firstId)).isNull();
        assertThat(service.getDao().findById(secondId)).isNull();
    }

    @Test
    void crudAbilityShouldUseCurrentVersionForHardDelete() {
        DemoPlainRecordService service = new DemoPlainRecordService();
        DemoPlainRecord record = new DemoPlainRecord("Versioned hard delete");
        String id = service.insert(record);
        service.update(record);

        assertThat(service.delete(id)).isEqualTo(1);

        assertThat(service.rawDao().lastDeleteConditions()).containsEntry("version", 1);
        assertThat(service.getDao().findById(id)).isNull();
    }

    @Test
    void baseDaoVersionMethodsShouldRequireExpectedVersion() {
        DemoPlainRecordService service = new DemoPlainRecordService();
        DemoPlainRecord record = new DemoPlainRecord("Versioned DAO");
        service.insert(record);

        assertThatThrownBy(() -> service.rawDao().updateByIdAndVersion(record, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedVersion");
        assertThatThrownBy(() -> service.rawDao().deleteByIdAndVersion(record.getId(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedVersion");
    }

    @Test
    void childrenAbilityShouldInsertLoadReplaceAndCascadeDeleteChildren() {
        DemoInvoiceService invoiceService = new DemoInvoiceService();
        DemoInvoiceLine firstLine = new DemoInvoiceLine("First line");
        DemoInvoiceLine secondLine = new DemoInvoiceLine("Second line");
        DemoInvoice invoice = new DemoInvoice("Invoice", List.of(firstLine, secondLine));

        String invoiceId = invoiceService.insert(invoice);

        assertThat(firstLine.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(secondLine.getInvoiceId()).isEqualTo(invoiceId);

        invoice.setLines(null);
        assertThat(invoiceService.select(invoiceId).getLines())
                .extracting(DemoInvoiceLine::getTitle)
                .containsExactly("First line", "Second line");

        firstLine.setTitle("First line updated");
        DemoInvoiceLine thirdLine = new DemoInvoiceLine("Third line");
        invoice.setLines(List.of(firstLine, thirdLine));
        invoiceService.update(invoice);

        assertThat(invoiceService.lineService().select(firstLine.getId()).getTitle()).isEqualTo("First line updated");
        assertThat(invoiceService.lineService().select(secondLine.getId())).isNull();
        assertThat(invoiceService.lineService().select(thirdLine.getId())).isNotNull();

        invoiceService.delete(invoiceId);

        assertThat(invoiceService.select(invoiceId)).isNull();
        assertThat(invoiceService.lineService().select(firstLine.getId())).isNull();
        assertThat(invoiceService.lineService().select(thirdLine.getId())).isNull();
        assertThat(invoiceService.lineService().selectIgnoreSoftDelete(firstLine.getId())).isNotNull();
        assertThat(invoiceService.lineService().selectIgnoreSoftDelete(thirdLine.getId())).isNotNull();
    }

    @Test
    void childrenAbilityShouldKeepChildrenWhenPayloadIsNullAndClearWhenEmpty() {
        DemoInvoiceService invoiceService = new DemoInvoiceService();
        DemoInvoiceLine firstLine = new DemoInvoiceLine("First line");
        DemoInvoiceLine secondLine = new DemoInvoiceLine("Second line");
        DemoInvoice invoice = new DemoInvoice("Invoice", List.of(firstLine, secondLine));

        String invoiceId = invoiceService.insert(invoice);

        invoice.setLines(null);
        invoiceService.update(invoice);

        assertThat(invoiceService.lineService().select(firstLine.getId())).isNotNull();
        assertThat(invoiceService.lineService().select(secondLine.getId())).isNotNull();

        invoice.setLines(List.of());
        invoiceService.update(invoice);

        assertThat(invoiceService.lineService().select(firstLine.getId())).isNull();
        assertThat(invoiceService.lineService().select(secondLine.getId())).isNull();
        assertThat(invoiceService.select(invoiceId).getLines()).isEmpty();
    }

    @Test
    void platformLifecycleShouldNotDependOnBusinessHooksCallingSuper() {
        DemoInvoiceService invoiceService = new DemoInvoiceService();
        DemoInvoiceLine firstLine = new DemoInvoiceLine("First line");
        DemoInvoice invoice = new DemoInvoice("Invoice", List.of(firstLine));

        String invoiceId = invoiceService.insert(invoice);
        assertThat(firstLine.getInvoiceId()).isEqualTo(invoiceId);

        invoice.setLines(null);
        assertThat(invoiceService.select(invoiceId).getLines())
                .extracting(DemoInvoiceLine::getTitle)
                .containsExactly("First line");

        invoice.setLines(List.of());
        invoiceService.update(invoice);
        assertThat(invoiceService.lineService().select(firstLine.getId())).isNull();

        invoiceService.delete(invoiceId);
        assertThat(invoiceService.businessHookCount()).isEqualTo(4);
    }

    @Test
    void platformSelectShouldLoadChildrenWhenParentRecordComesFromCache() {
        DemoInvoiceService invoiceService = new DemoInvoiceService();
        DemoInvoiceLine firstLine = new DemoInvoiceLine("First line");
        DemoInvoice invoice = new DemoInvoice("Invoice", List.of(firstLine));
        String invoiceId = invoiceService.insert(invoice);

        invoice.setLines(null);
        DemoInvoice firstSelected = invoiceService.select(invoiceId);
        assertThat(firstSelected.getLines())
                .extracting(DemoInvoiceLine::getTitle)
                .containsExactly("First line");

        invoice.setTitle("Changed behind cache");
        DemoInvoice secondSelected = invoiceService.select(invoiceId);

        assertThat(secondSelected.getTitle()).isEqualTo("Invoice");
        assertThat(secondSelected.getLines())
                .extracting(DemoInvoiceLine::getTitle)
                .containsExactly("First line");
        assertThat(invoiceService.businessHookCount()).isEqualTo(3);
    }

    @Test
    void childrenAbilityShouldRejectDuplicateAndForeignChildIds() {
        DemoInvoiceService invoiceService = new DemoInvoiceService();
        DemoInvoice firstInvoice = new DemoInvoice("First invoice", List.of(new DemoInvoiceLine("First line")));
        DemoInvoice secondInvoice = new DemoInvoice("Second invoice", List.of(new DemoInvoiceLine("Second line")));
        invoiceService.insert(firstInvoice);
        invoiceService.insert(secondInvoice);
        DemoInvoiceLine firstLine = firstInvoice.getLines().getFirst();
        DemoInvoiceLine secondLine = secondInvoice.getLines().getFirst();

        firstInvoice.setLines(List.of(firstLine, firstLine));
        assertThatThrownBy(() -> invoiceService.update(firstInvoice))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("Duplicate child id");

        firstInvoice.setLines(List.of(secondLine));
        assertThatThrownBy(() -> invoiceService.update(firstInvoice))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("does not belong to parent");
    }

    @Test
    void childrenAbilityShouldKeepAggregationInsideCurrentTenant() {
        DemoInvoiceService invoiceService = new DemoInvoiceService();
        String tenantAInvoiceId;
        DemoInvoiceLine tenantBLine;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            tenantAInvoiceId = invoiceService.insert(new DemoInvoice("Tenant A invoice", List.of(new DemoInvoiceLine("Tenant A line"))));
        }
        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            DemoInvoice tenantBInvoice = new DemoInvoice("Tenant B invoice", List.of(new DemoInvoiceLine("Tenant B line")));
            invoiceService.insert(tenantBInvoice);
            tenantBLine = tenantBInvoice.getLines().getFirst();
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            DemoInvoice selected = invoiceService.select(tenantAInvoiceId);
            assertThat(selected.getLines())
                    .extracting(DemoInvoiceLine::getTitle)
                    .containsExactly("Tenant A line");
            selected.setLines(List.of(tenantBLine));
            assertThatThrownBy(() -> invoiceService.update(selected))
                    .isInstanceOf(AbilityException.class)
                    .hasMessageContaining("does not belong to parent");
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            assertThat(invoiceService.lineService().select(tenantBLine.getId()).getTitle()).isEqualTo("Tenant B line");
        }
    }

    @Test
    void childrenAbilityShouldRejectInvalidChildIdsOnParentInsert() {
        DemoInvoiceService invoiceService = new DemoInvoiceService();
        DemoInvoiceLine duplicateLine = new DemoInvoiceLine("Duplicate line");
        duplicateLine.setId("same-line");

        assertThatThrownBy(() -> invoiceService.insert(new DemoInvoice("Duplicate invoice", List.of(duplicateLine, duplicateLine))))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("Duplicate child id");

        DemoInvoice existingInvoice = new DemoInvoice("Existing invoice", List.of(new DemoInvoiceLine("Existing line")));
        invoiceService.insert(existingInvoice);
        DemoInvoiceLine existingLine = existingInvoice.getLines().getFirst();

        assertThatThrownBy(() -> invoiceService.insert(new DemoInvoice("Foreign invoice", List.of(existingLine))))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("does not belong to parent");
    }

    @Test
    void cacheAbilityShouldReturnCopiesAndInvalidateAfterChange() {
        DemoCachedPlainRecordService service = new DemoCachedPlainRecordService();
        DemoPlainRecord record = new DemoPlainRecord("Cached");
        String id = service.insert(record);

        DemoPlainRecord selected = service.select(id);
        selected.setTitle("Changed outside cache");
        service.rawDao().findById(id).setTitle("Changed behind cache");

        assertThat(service.select(id).getTitle()).isEqualTo("Cached");

        DemoPlainRecord updated = new DemoPlainRecord("Updated");
        updated.setId(id);
        service.update(updated);

        assertThat(service.select(id).getTitle()).isEqualTo("Updated");
        assertThat(service.afterChangedCount()).isEqualTo(2);
    }

    @Test
    void cacheAbilityShouldCacheAllAndHideSoftDeletedRows() {
        DemoCachedPlainRecordService service = new DemoCachedPlainRecordService();
        DemoPlainRecord first = new DemoPlainRecord("First");
        DemoPlainRecord second = new DemoPlainRecord("Second");
        String firstId = service.insert(first);
        service.insert(second);

        assertThat(service.selectAllWithCache()).extracting(DemoPlainRecord::getTitle)
                .containsExactly("First", "Second");

        service.rawDao().findById(firstId).setTitle("Changed behind all cache");
        assertThat(service.selectAllWithCache()).extracting(DemoPlainRecord::getTitle)
                .containsExactly("First", "Second");

        service.delete(firstId);

        assertThat(service.selectAllWithCache()).extracting(DemoPlainRecord::getTitle)
                .containsExactly("Second");
        assertThat(service.select(firstId)).isNull();
        assertThat(service.selectIgnoreSoftDelete(firstId)).isNotNull();
    }

    @Test
    void cacheAbilityShouldIsolateServicesWithSameModuleAlias() {
        DemoCachedPlainRecordService firstService = new DemoCachedPlainRecordService();
        DemoCachedPlainRecordService secondService = new DemoCachedPlainRecordService();
        DemoPlainRecord record = new DemoPlainRecord("First service only");
        String id = firstService.insert(record);

        assertThat(firstService.select(id)).isNotNull();

        assertThat(secondService.select(id)).isNull();
    }

    @Test
    void referencerAbilityShouldCollectReferenceIdsBySourceNamespace() {
        DemoReferencingRecordService service = new DemoReferencingRecordService();
        DemoReferencingRecord record = new DemoReferencingRecord("customer-1", "user-owner");
        record.setCreatedBy("user-creator");

        assertThat(service.collectReferenceIdsBySourceNamespace(record))
                .containsEntry("demo.customer", java.util.Set.of("customer-1"))
                .containsEntry("iam.user", java.util.Set.of("user-creator", "user-owner"));
    }

    @Test
    void crudAbilityShouldIncreaseVersionOnUpdate() {
        DemoOrganizationService service = new DemoOrganizationService();
        DemoOrganization organization = new DemoOrganization("Versioned", TreeAbility.ROOT_ID);
        service.insert(organization);

        service.update(organization);

        assertThat(organization.getVersion()).isEqualTo(1);
        assertThat(organization.getUpdatedAt()).isAfterOrEqualTo(organization.getCreatedAt());
    }

    @Test
    void crudAbilityShouldRejectStaleVersionUpdate() {
        DemoPlainRecordService service = new DemoPlainRecordService();
        DemoPlainRecord record = new DemoPlainRecord("Versioned");
        String id = service.insert(record);
        service.update(record);

        DemoPlainRecord stale = new DemoPlainRecord("Stale");
        stale.setId(id);
        stale.setVersion(0);

        assertThatThrownBy(() -> service.update(stale))
                .isInstanceOf(OptimisticLockException.class)
                .hasMessageContaining("version conflict");
        assertThat(service.select(id).getTitle()).isEqualTo("Versioned");
    }

    @Test
    void softDeleteAbilityShouldRejectStaleVersionDelete() {
        DemoCachedPlainRecordService service = new DemoCachedPlainRecordService();
        DemoPlainRecord record = new DemoPlainRecord("Versioned");
        String id = service.insert(record);
        service.update(record);

        DemoPlainRecord stale = new DemoPlainRecord("Stale");
        stale.setId(id);
        stale.setVersion(0);

        assertThatThrownBy(() -> service.delete(stale))
                .isInstanceOf(OptimisticLockException.class)
                .hasMessageContaining("version conflict");
        assertThat(service.select(id)).isNotNull();
    }

    @Test
    void treeAbilityShouldResolveChildrenAndAncestors() {
        DemoOrganizationService service = new DemoOrganizationService();
        DemoOrganization rootChild = new DemoOrganization("Region", TreeAbility.ROOT_ID);
        DemoOrganization leaf = new DemoOrganization("Branch", null);
        DemoOrganization subLeaf = new DemoOrganization("Desk", null);

        String regionId = service.insert(rootChild);
        leaf.setParentId(regionId);
        String leafId = service.insert(leaf);
        subLeaf.setParentId(leafId);
        String subLeafId = service.insert(subLeaf);

        assertThat(service.children(regionId)).containsExactly(leaf);
        assertThat(service.ancestorIds(leafId)).containsExactly(regionId);
        assertThat(service.ancestorIdsAndSelf(leafId)).containsExactly(regionId, leafId);
        assertThat(service.ancestorIds(subLeafId)).containsExactly(regionId, leafId);
        assertThat(service.descendantIds(regionId)).containsExactly(leafId, subLeafId);
        assertThat(service.ancestorIdsAndSelf("missing")).isEmpty();
    }

    @Test
    void treeAbilityShouldRejectCycles() {
        DemoOrganizationService service = new DemoOrganizationService();
        String parentId = service.insert(new DemoOrganization("Parent", TreeAbility.ROOT_ID));
        DemoOrganization child = new DemoOrganization("Child", parentId);
        String childId = service.insert(child);

        DemoOrganization parent = service.select(parentId);
        parent.setParentId(childId);

        assertThatThrownBy(() -> service.update(parent))
                .isInstanceOf(AbilityException.class);
    }

    @Test
    void treeAbilityShouldRejectMissingParent() {
        DemoOrganizationService service = new DemoOrganizationService();

        assertThatThrownBy(() -> service.insert(new DemoOrganization("Orphan", "missing-parent")))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("missing parent");
    }

    @Test
    void treeAbilityShouldRejectCorruptDescendantCycles() {
        DemoOrganizationService service = new DemoOrganizationService();
        DemoOrganization first = new DemoOrganization("First", null);
        DemoOrganization second = new DemoOrganization("Second", null);

        String firstId = service.insert(first);
        second.setParentId(firstId);
        String secondId = service.insert(second);
        first.setParentId(secondId);

        assertThatThrownBy(() -> service.descendantIds(firstId))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("Tree cycle");
    }

    @Test
    void sortAbilityShouldReorderAndMoveRecords() {
        DemoOrganizationService service = new DemoOrganizationService();
        String first = service.insert(new DemoOrganization("First", TreeAbility.ROOT_ID));
        String second = service.insert(new DemoOrganization("Second", TreeAbility.ROOT_ID));
        String third = service.insert(new DemoOrganization("Third", TreeAbility.ROOT_ID));

        service.reorder(List.of(first, second, third));
        service.moveBefore(third, first);

        assertThat(service.sortedList(Criteria.of()).stream().map(DemoOrganization::getId))
                .containsExactly(third, first, second);

        service.moveAfter(first, second);

        assertThat(service.sortedList(Criteria.of()).stream().map(DemoOrganization::getId))
                .containsExactly(third, second, first);
    }

    @Test
    void sortAbilityShouldKeepReorderScopeInsideCurrentTenant() {
        DemoOrganizationService service = new DemoOrganizationService();
        String tenantAFirst;
        String tenantASecond;
        String tenantBFirst;
        String tenantBSecond;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            tenantAFirst = service.insert(new DemoOrganization("Tenant A First", TreeAbility.ROOT_ID));
            tenantASecond = service.insert(new DemoOrganization("Tenant A Second", TreeAbility.ROOT_ID));
        }
        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            tenantBFirst = service.insert(new DemoOrganization("Tenant B First", TreeAbility.ROOT_ID));
            tenantBSecond = service.insert(new DemoOrganization("Tenant B Second", TreeAbility.ROOT_ID));
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            service.reorder(List.of(tenantASecond, tenantAFirst));
            assertThat(service.sortedList(Criteria.of()).stream().map(DemoOrganization::getId))
                    .containsExactly(tenantASecond, tenantAFirst);
            assertThatThrownBy(() -> service.reorder(List.of(tenantAFirst, tenantBFirst)))
                    .isInstanceOf(AbilityException.class)
                    .hasMessageContaining("missing record");
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            assertThat(service.sortedList(Criteria.of()).stream().map(DemoOrganization::getId))
                    .containsExactly(tenantBFirst, tenantBSecond);
            service.moveAfter(tenantBFirst, tenantBSecond);
            assertThat(service.sortedList(Criteria.of()).stream().map(DemoOrganization::getId))
                    .containsExactly(tenantBSecond, tenantBFirst);
        }
    }

    @Test
    void sortAbilityShouldRejectCrossParentTreeMove() {
        DemoOrganizationService service = new DemoOrganizationService();
        String firstParent = service.insert(new DemoOrganization("First Parent", TreeAbility.ROOT_ID));
        String secondParent = service.insert(new DemoOrganization("Second Parent", TreeAbility.ROOT_ID));
        String firstChild = service.insert(new DemoOrganization("First Child", firstParent));
        String secondChild = service.insert(new DemoOrganization("Second Child", secondParent));

        assertThatThrownBy(() -> service.moveBefore(firstChild, secondChild))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("same parent");
    }

    @Test
    void sortAbilityShouldRejectDuplicateReorderIds() {
        DemoOrganizationService service = new DemoOrganizationService();
        String id = service.insert(new DemoOrganization("Duplicate", TreeAbility.ROOT_ID));

        assertThatThrownBy(() -> service.reorder(List.of(id, id)))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void sortAbilityShouldRejectEmptyAndCrossParentReorder() {
        DemoOrganizationService service = new DemoOrganizationService();
        String firstParent = service.insert(new DemoOrganization("First Parent", TreeAbility.ROOT_ID));
        String secondParent = service.insert(new DemoOrganization("Second Parent", TreeAbility.ROOT_ID));
        String firstChild = service.insert(new DemoOrganization("First Child", firstParent));
        String secondChild = service.insert(new DemoOrganization("Second Child", secondParent));

        assertThatThrownBy(() -> service.reorder(List.of()))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("empty");

        assertThatThrownBy(() -> service.reorder(List.of(firstChild, secondChild)))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("same parent");
    }

    @Test
    void sortAbilityShouldRejectPartialReorderScope() {
        DemoOrganizationService service = new DemoOrganizationService();
        String first = service.insert(new DemoOrganization("First", TreeAbility.ROOT_ID));
        String second = service.insert(new DemoOrganization("Second", TreeAbility.ROOT_ID));
        String third = service.insert(new DemoOrganization("Third", TreeAbility.ROOT_ID));

        assertThatThrownBy(() -> service.reorder(List.of(third, first)))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("complete scope");

        assertThat(service.sortedList(Criteria.of()).stream().map(DemoOrganization::getId))
                .containsExactly(first, second, third);
    }

    @Test
    void referenceAbilityShouldResolveTitles() {
        DemoOrganizationService service = new DemoOrganizationService();
        String id = service.insert(new DemoOrganization("Reference Title", TreeAbility.ROOT_ID));
        String secondId = service.insert(new DemoOrganization("Second Title", TreeAbility.ROOT_ID));

        assertThat(service.title(id)).isEqualTo("Reference Title");
        assertThat(service.titles(java.util.List.of(secondId, id)))
                .containsExactly(
                        Map.entry(secondId, "Second Title"),
                        Map.entry(id, "Reference Title")
                );
        assertThat(service.referenceOptions(Criteria.of(), PageRequest.of(1, 10)).getRecords())
                .containsExactly(
                        new ReferenceOption(id, "Reference Title"),
                        new ReferenceOption(secondId, "Second Title")
                );
    }

    @Test
    void referenceAbilityShouldKeepTitlesAndOptionsInsideCurrentTenant() {
        DemoOrganizationService service = new DemoOrganizationService();
        String tenantAId;
        String tenantBId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            tenantAId = service.insert(new DemoOrganization("Tenant A", TreeAbility.ROOT_ID));
        }
        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            tenantBId = service.insert(new DemoOrganization("Tenant B", TreeAbility.ROOT_ID));
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(service.title(tenantAId)).isEqualTo("Tenant A");
            assertThat(service.title(tenantBId)).isNull();
            assertThat(service.titles(List.of(tenantBId, tenantAId)))
                    .containsExactly(Map.entry(tenantAId, "Tenant A"));
            assertThat(service.referenceOptions(Criteria.of(), PageRequest.of(1, 10)).getRecords())
                    .containsExactly(new ReferenceOption(tenantAId, "Tenant A"));
        }
    }

    @Test
    void referenceAbilityShouldHideDeletedTitlesInBatch() {
        DemoOrganizationService service = new DemoOrganizationService();
        String activeId = service.insert(new DemoOrganization("Active", TreeAbility.ROOT_ID));
        String deletedId = service.insert(new DemoOrganization("Deleted", TreeAbility.ROOT_ID));
        service.delete(deletedId);

        assertThat(service.titles(List.of(deletedId, activeId)))
                .containsExactly(Map.entry(activeId, "Active"));
    }

    @Test
    void pageQueryShouldHideSoftDeletedRows() {
        DemoOrganizationService service = new DemoOrganizationService();
        String activeId = service.insert(new DemoOrganization("Active", TreeAbility.ROOT_ID));
        DemoOrganization nullDeleted = new DemoOrganization("Null Deleted", TreeAbility.ROOT_ID);
        String nullDeletedId = service.insert(nullDeleted);
        nullDeleted.setDeleted(null);
        String deletedId = service.insert(new DemoOrganization("Deleted", TreeAbility.ROOT_ID));
        service.delete(deletedId);

        assertThat(service.pageQuery(Criteria.of(), PageRequest.of(1, 10)).getRecords())
                .extracting(DemoOrganization::getId)
                .containsExactly(activeId, nullDeletedId);
    }

    @Test
    void abilityQueriesShouldNotMutateCallerCriteria() {
        DemoOrganizationService service = new DemoOrganizationService();
        Criteria criteria = Criteria.of().eq("parentId", TreeAbility.ROOT_ID);

        service.pageQuery(criteria, PageRequest.of(1, 10));
        service.count(criteria);
        service.sortedList(criteria);

        assertThat(criteria.getClauses())
                .extracting(clause -> clause.getField() + ":" + clause.getOperator())
                .containsExactly("parentId:EQ");
    }
}
