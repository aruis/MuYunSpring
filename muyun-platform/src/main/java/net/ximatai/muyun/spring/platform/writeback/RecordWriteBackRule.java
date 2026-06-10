package net.ximatai.muyun.spring.platform.writeback;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Indexed;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationEventType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Table(name = "platform_record_write_back_rule", comment = "Record write-back rule")
public class RecordWriteBackRule extends StandardEnabledSortableEntity {
    @Indexed
    @Column(name = "trigger_module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false,
            comment = "Trigger module alias")
    private String triggerModuleAlias;

    @Indexed
    @Column(name = "target_module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false,
            comment = "Target module alias")
    private String targetModuleAlias;

    @Column(name = "event_type", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Mutation event type")
    private DynamicRecordMutationEventType eventType = DynamicRecordMutationEventType.AFTER_SAVE;

    @Column(name = "cascade_mode", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Cascade mode")
    private RecordWriteBackCascadeMode cascadeMode = RecordWriteBackCascadeMode.SINGLE_HOP;

    @Column(name = "trigger_mode", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Trigger mode")
    private RecordWriteBackTriggerMode triggerMode = RecordWriteBackTriggerMode.ALWAYS;

    @Column(name = "trigger_modes", type = ColumnType.TEXT, comment = "Trigger modes")
    private String triggerModes;

    @Column(name = "trigger_field", type = ColumnType.VARCHAR, length = 64, comment = "Trigger field")
    private String triggerField;

    @Column(name = "trigger_value", type = ColumnType.VARCHAR, length = 128, comment = "Trigger value")
    private String triggerValue;

    @Column(name = "target_locate_mode", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Target locate mode")
    private RecordWriteBackTargetLocateMode targetLocateMode = RecordWriteBackTargetLocateMode.FIELD_MATCH;

    @Column(name = "relation_generation_rule_id", type = ColumnType.VARCHAR, length = 64,
            comment = "Generation rule id for relation locate")
    private String relationGenerationRuleId;

    @Column(name = "target_relation_code", type = ColumnType.VARCHAR, length = 64,
            comment = "Target child relation code")
    private String targetRelationCode;

    @Column(name = "target_entity_alias", type = ColumnType.VARCHAR, length = 64,
            comment = "Target child entity alias")
    private String targetEntityAlias;

    private transient List<RecordWriteBackMatchRule> matchRules = new ArrayList<>();
    private transient List<RecordWriteBackFieldRule> fieldRules = new ArrayList<>();
}
