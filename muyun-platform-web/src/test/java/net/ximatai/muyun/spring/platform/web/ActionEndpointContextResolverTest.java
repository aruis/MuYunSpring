package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;

class ActionEndpointContextResolverTest {

    @Test
    void shouldResolveActionPolicyToTheModuleAnchoredByIndependentStaticActionScope() throws Exception {
        ProjectionController controller = new ProjectionController();
        HandlerMethod handler = new HandlerMethod(controller,
                ProjectionController.class.getMethod("issueTransferTicket"));
        ActionEndpoint endpoint = handler.getMethodAnnotation(ActionEndpoint.class);

        var context = new ActionEndpointContextResolver()
                .resolve(new MockHttpServletRequest(), handler, endpoint)
                .orElseThrow();

        assertThat(context.moduleAlias()).isEqualTo("mr.knowledge_file");
        assertThat(context.actionCode()).isEqualTo(PlatformAction.CREATE.code());
    }

    @PlatformStaticActionScope(module = "mr.knowledge_file")
    static class ProjectionController {
        @ActionEndpoint(PlatformAction.CREATE)
        public void issueTransferTicket() {
        }
    }
}
