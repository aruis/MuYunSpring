package net.ximatai.muyun.spring.platform.measure;

import java.time.LocalDateTime;

public record MeasureUnitConversionContext(
        String applicationAlias,
        String moduleAlias,
        String contextObjectType,
        String contextObjectId,
        LocalDateTime operatedAt
) {
}
