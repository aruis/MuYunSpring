package net.ximatai.muyun.spring.dynamic.web;

import net.ximatai.muyun.spring.platform.web.DynamicRelationProjectionReadService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshotService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryItemService;

import java.util.Objects;

public record DynamicRecordQueryServices(
        PlatformPageConfigSnapshotService pageConfigSnapshotService,
        PlatformQueryItemService queryItemService,
        ModuleMetadataFieldService moduleMetadataFieldService,
        DynamicRelationProjectionReadService relationProjectionReadService
) {
    public DynamicRecordQueryServices {
        Objects.requireNonNull(relationProjectionReadService, "relationProjectionReadService");
    }
}
