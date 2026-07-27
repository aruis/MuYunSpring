package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.time.Instant;

/** One retained resource together with the lifecycle source usable for recovery. */
public record RecycleBinItem<T extends EntityContract>(T record,
                                                        String sourceDeleteOperationId,
                                                        Instant deletedAt,
                                                        boolean restorable,
                                                        String unavailableReason) {
}
