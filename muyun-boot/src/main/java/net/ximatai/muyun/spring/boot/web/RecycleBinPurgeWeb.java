package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.deletion.PurgeReport;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Explicit HTTP opt-in for irreversible recycle-bin cleanup.
 *
 * <p>Query and restore are ordinary recycle-bin operations. Purge has a
 * materially different risk boundary, so a resource controller must expose
 * it separately in addition to enabling purge in its service.</p>
 */
public interface RecycleBinPurgeWeb<T extends EntityContract, S extends RecycleBinAbility<T>>
        extends RecycleBinWeb<T, S> {

    @PostMapping("/recycle-bin/{sourceDeleteOperationId}/purge")
    @ActionEndpoint(PlatformAction.RECYCLE_BIN_PURGE)
    default PurgeReport purgeFromRecycleBin(@PathVariable String sourceDeleteOperationId) {
        return webScope(() -> recycleBinFacade().purge(service(), sourceDeleteOperationId));
    }
}
