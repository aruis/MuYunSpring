package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.boot.dynamic.DynamicRecordWebController;
import net.ximatai.muyun.spring.boot.iam.UserAccountWebController;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ActionEndpointAnnotationTest {
    @Test
    void shouldDescribeStandardCrudEndpointActionSemantics() throws Exception {
        assertThat(endpoint(CrudWeb.class, "query", WebQueryRequest.class).value()).isEqualTo(PlatformAction.QUERY);
        assertThat(endpoint(CrudWeb.class, "view", String.class).value()).isEqualTo(PlatformAction.VIEW);
        assertThat(endpoint(CrudWeb.class, "insert", EntityContract.class).value()).isEqualTo(PlatformAction.CREATE);
        assertThat(endpoint(CrudWeb.class, "update", String.class, EntityContract.class).value()).isEqualTo(PlatformAction.UPDATE);
        assertThat(endpoint(CrudWeb.class, "delete", String.class).value()).isEqualTo(PlatformAction.DELETE);
    }

    @Test
    void shouldDescribeAbilityEndpointActionSemantics() throws Exception {
        assertThat(endpoint(EnableWeb.class, "enable", String.class).value()).isEqualTo(PlatformAction.ENABLE);
        assertThat(endpoint(EnableWeb.class, "disable", String.class).value()).isEqualTo(PlatformAction.DISABLE);
        assertThat(endpoint(SortWeb.class, "sort", String.class, SortWebRequest.class).value()).isEqualTo(PlatformAction.SORT);
        assertThat(endpoint(TreeWeb.class, "tree", boolean.class).value()).isEqualTo(PlatformAction.TREE);
        assertThat(endpoint(ReferenceWeb.class, "reference", String.class, Object.class).value())
                .isEqualTo(PlatformAction.REFERENCE);
    }

    @Test
    void shouldNotTreatActionListEndpointsAsBusinessActionEndpoints() throws Exception {
        assertThat(ActionWeb.class.getMethod("actions").getAnnotation(ActionEndpoint.class)).isNull();
        assertThat(ActionWeb.class.getMethod("recordActions", String.class).getAnnotation(ActionEndpoint.class)).isNull();
    }

    @Test
    void shouldKeepActionEndpointWhenDynamicControllerOverridesStandardWebMethods() throws Exception {
        assertThat(endpoint(DynamicRecordWebController.class, "sort", String.class, TreeSortWebRequest.class).value())
                .isEqualTo(PlatformAction.SORT);
        Class<?> referenceRequestType = Class.forName("net.ximatai.muyun.spring.boot.dynamic.DynamicWebReferenceRequest");
        assertThat(endpoint(DynamicRecordWebController.class, "reference", String.class, referenceRequestType).value())
                .isEqualTo(PlatformAction.REFERENCE);
    }

    @Test
    void shouldDescribeUserManagementEndpointActionSemantics() throws Exception {
        CustomActionEndpoint endpoint = customEndpoint(UserAccountWebController.class, "changePassword",
                String.class, UserAccountWebController.ChangePasswordRequest.class);
        assertThat(endpoint.value()).isEqualTo("changePassword");
        assertThat(endpoint.level()).isEqualTo(PlatformActionLevel.RECORD);
        assertThat(endpoint.dataAuth()).isTrue();
        assertThat(endpoint.recordIdPathVariable()).isEqualTo("id");
    }

    private ActionEndpoint endpoint(Class<?> type, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = type.getMethod(methodName, parameterTypes);
        return method.getAnnotation(ActionEndpoint.class);
    }

    private CustomActionEndpoint customEndpoint(Class<?> type, String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = type.getMethod(methodName, parameterTypes);
        return method.getAnnotation(CustomActionEndpoint.class);
    }
}
