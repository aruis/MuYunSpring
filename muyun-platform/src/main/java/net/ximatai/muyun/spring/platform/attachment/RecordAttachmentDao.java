package net.ximatai.muyun.spring.platform.attachment;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface RecordAttachmentDao extends BaseDao<RecordAttachment, String> {
}
