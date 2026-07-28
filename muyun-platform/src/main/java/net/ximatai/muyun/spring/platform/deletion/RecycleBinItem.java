package net.ximatai.muyun.spring.platform.deletion;

import java.time.Instant;

/** One retained resource together with the lifecycle source usable for recovery. */
public record RecycleBinItem<T>(T record,
                                                        String sourceDeleteOperationId,
                                                        Instant deletedAt,
                                                        boolean restorable,
                                                        boolean purgeable,
                                                        String unavailableReason) {
}
