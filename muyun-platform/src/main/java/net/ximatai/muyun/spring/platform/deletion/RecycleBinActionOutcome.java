package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/** The retained root record together with one recycle-bin action report. */
public record RecycleBinActionOutcome<T extends EntityContract, R>(String recordId, T record, R report) {
}
