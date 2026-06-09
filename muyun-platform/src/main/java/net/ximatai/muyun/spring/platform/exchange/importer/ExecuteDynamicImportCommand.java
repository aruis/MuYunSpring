package net.ximatai.muyun.spring.platform.exchange.importer;

import net.ximatai.muyun.spring.common.exception.PlatformException;

public record ExecuteDynamicImportCommand(
        DynamicImportPlan plan,
        GroupedWorkbook workbook
) {
    public ExecuteDynamicImportCommand {
        if (plan == null) {
            throw new PlatformException("dynamic import execution plan must not be null");
        }
        if (workbook == null) {
            throw new PlatformException("dynamic import grouped workbook must not be null");
        }
    }
}
