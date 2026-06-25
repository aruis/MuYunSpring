package net.ximatai.muyun.spring.iam.position;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PositionCategoryServiceContractTest {
    @Test
    void shouldExposeStableModuleAlias() {
        PositionCategoryService service = new PositionCategoryService(mock(PositionCategoryDao.class),
                activeTenantVerifier());

        assertThat(service.getModuleAlias()).isEqualTo("iam.position_category");
    }

    @Test
    void shouldFillPositionCategoryDefaultsThroughCrudAbility() {
        PositionCategoryDao dao = mock(PositionCategoryDao.class);
        when(dao.insert(any())).thenReturn("position-category-1");
        ActiveTenantVerifier tenantVerifier = activeTenantVerifier();
        PositionCategoryService service = new PositionCategoryService(dao, tenantVerifier);
        PositionCategory category = category("TECH", "Technology");
        category.setDescription(" ");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.insert(category);
        }

        assertThat(category.getEnabled()).isTrue();
        assertThat(category.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
        assertThat(category.getTenantId()).isEqualTo("tenant_a");
        assertThat(category.getDescription()).isNull();
        verify(tenantVerifier).verifyActiveTenant("tenant_a");
    }

    @Test
    void shouldRequireTenantContextForPositionCategoryMutation() {
        PositionCategoryService service = new PositionCategoryService(mock(PositionCategoryDao.class),
                activeTenantVerifier());

        assertThatThrownBy(() -> service.insert(category("TECH", "Technology")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("tenant context");
    }

    @Test
    void shouldRequirePositionCategoryCodeAndTitle() {
        PositionCategoryService service = new PositionCategoryService(mock(PositionCategoryDao.class),
                activeTenantVerifier());

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.insert(category(" ", "Technology")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positionCategoryCode");
            assertThatThrownBy(() -> service.insert(category("TECH", " ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positionCategoryTitle");
        }
    }

    private PositionCategory category(String code, String title) {
        PositionCategory category = new PositionCategory();
        category.setCode(code);
        category.setTitle(title);
        return category;
    }

    private ActiveTenantVerifier activeTenantVerifier() {
        return mock(ActiveTenantVerifier.class);
    }
}
