package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.deletion.RecycleBinFacade;
import net.ximatai.muyun.spring.platform.deletion.RecycleBinItem;
import net.ximatai.muyun.spring.platform.deletion.RestoreReport;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * HTTP adapter for the optional operator-facing recycle-bin lifecycle.
 *
 * <p>Resource services opt in through {@link RecycleBinAbility}; the platform
 * facade retains ownership of lifecycle history validation and recovery-tree
 * execution. A controller only supplies its standard service and the shared
 * facade, so every opted-in module exposes the same action and response
 * contract.</p>
 */
public interface RecycleBinWeb<T extends EntityContract, S extends RecycleBinAbility<T>> extends ScopedWeb<S> {
    /** Shared lifecycle facade supplied by the platform application context. */
    RecycleBinFacade recycleBinFacade();

    @PostMapping("/recycle-bin/query")
    @ActionEndpoint(PlatformAction.RECYCLE_BIN_QUERY)
    default WebListResponse<RecycleBinItem<T>> recycleBin(@RequestBody(required = false) WebPageRequest request) {
        return webScope(() -> {
            WebPageRequest page = request == null ? WebPageRequest.DEFAULT : request;
            return new WebListResponse<>(recycleBinFacade().list(service(),
                    PageRequest.of(page.pageNum(), page.pageSize())));
        });
    }

    @PostMapping("/recycle-bin/{sourceDeleteOperationId}/restore")
    @ActionEndpoint(PlatformAction.RECYCLE_BIN_RESTORE)
    default RestoreReport restoreFromRecycleBin(@PathVariable String sourceDeleteOperationId) {
        return webScope(() -> recycleBinFacade().restore(service(), sourceDeleteOperationId));
    }
}
