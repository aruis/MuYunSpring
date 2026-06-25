package net.ximatai.muyun.spring.iam.position;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PositionServiceContractTest {
    @Test
    void shouldExposeStableModuleAlias() {
        PositionService service = new PositionService(mock(PositionDao.class), activeTenantVerifier(),
                positionCategoryService());

        assertThat(service.getModuleAlias()).isEqualTo("iam.position");
    }

    @Test
    void shouldFillPositionDefaultsThroughCrudAbility() {
        PositionDao dao = mock(PositionDao.class);
        when(dao.insert(any())).thenReturn("position-1");
        ActiveTenantVerifier tenantVerifier = activeTenantVerifier();
        PositionService service = new PositionService(dao, tenantVerifier, positionCategoryService());
        Position position = position("SALES_MANAGER", "Sales Manager");
        position.setDescription(" ");
        position.setCategoryId(" ");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.insert(position);
        }

        assertThat(position.getEnabled()).isTrue();
        assertThat(position.getTenantId()).isEqualTo("tenant_a");
        assertThat(position.getCategoryId()).isNull();
        assertThat(position.getDescription()).isNull();
        verify(tenantVerifier).verifyActiveTenant("tenant_a");
    }

    @Test
    void shouldRequireTenantContextForPositionMutation() {
        PositionService service = new PositionService(mock(PositionDao.class), activeTenantVerifier(),
                positionCategoryService());

        assertThatThrownBy(() -> service.insert(position("SALES_MANAGER", "Sales Manager")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("tenant context");
    }

    @Test
    void shouldRequirePositionCodeAndTitle() {
        PositionService service = new PositionService(mock(PositionDao.class), activeTenantVerifier(),
                positionCategoryService());

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.insert(position(" ", "Sales Manager")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positionCode");
            assertThatThrownBy(() -> service.insert(position("SALES_MANAGER", " ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positionTitle");
        }
    }

    @Test
    void shouldRequireActivePositionCategoryWhenPresent() {
        PositionDao dao = mock(PositionDao.class);
        when(dao.insert(any())).thenReturn("position-1");
        PositionCategoryService categoryService = positionCategoryService();
        PositionCategory category = new PositionCategory();
        category.setId("category-1");
        when(categoryService.requireEnabled(eq("category-1"), any())).thenReturn(category);
        PositionService service = new PositionService(dao, activeTenantVerifier(), categoryService);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            Position position = position("SALES_MANAGER", "Sales Manager");
            position.setCategoryId(" category-1 ");
            service.insert(position);

            assertThat(position.getCategoryId()).isEqualTo("category-1");
            verify(categoryService).requireEnabled(eq("category-1"), any());
        }
    }

    @Test
    void shouldUseCategorySortScopeForPositionCatalog() {
        PositionService service = new PositionService(mock(PositionDao.class), activeTenantVerifier(),
                positionCategoryService());
        Position uncategorizedLeft = position("SALES_MANAGER", "Sales Manager");
        Position uncategorizedRight = position("SALES_DIRECTOR", "Sales Director");
        Position sameCategoryLeft = position("FINANCE_REVIEWER", "Finance Reviewer");
        sameCategoryLeft.setCategoryId("category-1");
        Position sameCategoryRight = position("FINANCE_MANAGER", "Finance Manager");
        sameCategoryRight.setCategoryId("category-1");
        Position anotherCategory = position("TECH_LEAD", "Tech Lead");
        anotherCategory.setCategoryId("category-2");

        assertThat(service.sortScope(uncategorizedLeft).isEmpty()).isFalse();
        assertThat(service.sortScope(sameCategoryLeft).isEmpty()).isFalse();
        service.validateSortScope(uncategorizedLeft, uncategorizedRight);
        service.validateSortScope(sameCategoryLeft, sameCategoryRight);
        assertThatThrownBy(() -> service.validateSortScope(uncategorizedLeft, sameCategoryLeft))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("same category");
        assertThatThrownBy(() -> service.validateSortScope(sameCategoryLeft, anotherCategory))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("same category");
    }

    private Position position(String code, String title) {
        Position position = new Position();
        position.setCode(code);
        position.setTitle(title);
        return position;
    }

    private ActiveTenantVerifier activeTenantVerifier() {
        return mock(ActiveTenantVerifier.class);
    }

    private PositionCategoryService positionCategoryService() {
        return mock(PositionCategoryService.class);
    }
}
