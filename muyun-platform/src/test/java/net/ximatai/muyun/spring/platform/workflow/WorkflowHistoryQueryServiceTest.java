package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowHistoryQueryServiceTest {
    private final WorkflowHistoryInstanceDao historyDao = mock(WorkflowHistoryInstanceDao.class);
    private final WorkflowArchiveService archiveService = mock(WorkflowArchiveService.class);
    private final WorkflowHistoryQueryService service = new WorkflowHistoryQueryService(historyDao, archiveService);

    @Test
    void shouldQueryRecordHistoryByModuleAndRecord() {
        when(historyDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of());

        service.queryRecordHistory("sales.contract", "record-1", PageRequest.of(1, 20));

        verify(historyDao).query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class));
    }

    @Test
    void shouldRejectMissingHistoryInstance() {
        assertThatThrownBy(() -> service.renderBundle("missing"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("workflow history instance not found");
    }
}
