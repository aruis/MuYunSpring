package net.ximatai.muyun.spring.platform;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryKind;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import net.ximatai.muyun.spring.platform.metadata.RelationRole;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAddSignMode;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAssignmentKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowApprovalMode;
import net.ximatai.muyun.spring.platform.workflow.WorkflowApprovalStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowConvergeMode;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinitionStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowEventType;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowMilestoneType;
import net.ximatai.muyun.spring.platform.workflow.WorkflowNodeStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowNodeType;
import net.ximatai.muyun.spring.platform.workflow.WorkflowOvertimeStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowPublishStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRejectResubmitMode;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRouteReason;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRouteStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskCheckKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskCheckStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskGuideKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformEnumContractTest {
    @Test
    void shouldExposeStableCodeAndTitleForPlatformEnums() {
        assertEnumContract(ModuleKind.class);
        assertEnumContract(RelationRole.class);
        assertEnumContract(MenuScopeType.class);
        assertEnumContract(MenuType.class);
        assertEnumContract(DictionaryCategoryKind.class);
        assertEnumContract(WorkflowDefinitionStatus.class);
        assertEnumContract(WorkflowAssignmentKind.class);
        assertEnumContract(WorkflowAddSignMode.class);
        assertEnumContract(WorkflowPublishStatus.class);
        assertEnumContract(WorkflowNodeType.class);
        assertEnumContract(WorkflowMilestoneType.class);
        assertEnumContract(WorkflowConvergeMode.class);
        assertEnumContract(WorkflowApprovalMode.class);
        assertEnumContract(WorkflowInstanceStatus.class);
        assertEnumContract(WorkflowApprovalStatus.class);
        assertEnumContract(WorkflowRejectResubmitMode.class);
        assertEnumContract(WorkflowNodeStatus.class);
        assertEnumContract(WorkflowRouteStatus.class);
        assertEnumContract(WorkflowRouteReason.class);
        assertEnumContract(WorkflowTaskKind.class);
        assertEnumContract(WorkflowTaskStatus.class);
        assertEnumContract(WorkflowEventType.class);
        assertEnumContract(WorkflowOvertimeStatus.class);
        assertEnumContract(WorkflowTaskGuideKind.class);
        assertEnumContract(WorkflowTaskCheckKind.class);
        assertEnumContract(WorkflowTaskCheckStatus.class);
    }

    private <E extends Enum<E> & CodeTitleEnum> void assertEnumContract(Class<E> enumType) {
        assertThat(Arrays.stream(enumType.getEnumConstants()).map(CodeTitleEnum::getCode))
                .allSatisfy(code -> assertThat(code).isNotBlank())
                .doesNotHaveDuplicates();
        assertThat(Arrays.stream(enumType.getEnumConstants()).map(CodeTitleEnum::getTitle))
                .allSatisfy(title -> assertThat(title).isNotBlank());
    }
}
