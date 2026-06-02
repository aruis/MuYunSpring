package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldDefinitionTest {
    @Test
    void shouldExposeDictionaryBindingAsUnifiedOptionBinding() {
        FieldDefinition field = FieldDefinition.string("status", "Status")
                .dictionary("crm", "customer_status");

        assertThat(field.dictionaryBinding())
                .isEqualTo(new FieldDictionaryBinding("crm", "customer_status"));
        assertThat(field.dictionaryBinding().selectionMode()).isEqualTo(OptionSelectionMode.SINGLE);
        assertThat(field.optionBinding())
                .isEqualTo(OptionBinding.dictionary("crm", "customer_status"));
    }

    @Test
    void shouldExposeMultipleDictionarySelectionMode() {
        FieldDefinition field = FieldDefinition.of("tags", FieldType.JSON, "Tags")
                .dictionary("crm", "customer_tag", OptionSelectionMode.MULTIPLE);

        assertThat(field.dictionaryBinding().selectionMode()).isEqualTo(OptionSelectionMode.MULTIPLE);
    }

    @Test
    void shouldExposeQueryDefinitionOnField() {
        FieldDefinition title = FieldDefinition.titleField().queryable();
        FieldDefinition status = FieldDefinition.string("status", "Status")
                .queryable(DynamicQueryOperator.EQ, Set.of(DynamicQueryOperator.EQ, DynamicQueryOperator.IN));

        assertThat(title.queryDefinition().queryable()).isTrue();
        assertThat(title.queryDefinition().defaultOperator()).isEqualTo(DynamicQueryOperator.LIKE);
        assertThat(status.queryDefinition().operators()).containsExactlyInAnyOrder(DynamicQueryOperator.EQ, DynamicQueryOperator.IN);
    }

    @Test
    void shouldExposeFieldBehaviorDefinition() {
        FieldDefinition field = FieldDefinition.string("code", "Code")
                .defaultValue("AUTO")
                .validationRegex("[A-Z]+")
                .notCopyable()
                .writeProtected();

        assertThat(field.behavior().defaultValue()).isEqualTo("AUTO");
        assertThat(field.behavior().validationRegex()).isEqualTo("[A-Z]+");
        assertThat(field.behavior().copyable()).isFalse();
        assertThat(field.behavior().writeProtected()).isTrue();
    }

    @Test
    void shouldRejectQueryOperatorUnsupportedByFieldType() {
        assertThatThrownBy(() -> FieldDefinition.decimal("amount", "Amount")
                .queryable(DynamicQueryOperator.LIKE, Set.of(DynamicQueryOperator.LIKE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void shouldRejectInvalidFieldBehaviorDefinition() {
        ModuleDefinitionValidator validator = new ModuleDefinitionValidator();

        assertThatThrownBy(() -> validator.validateEntity(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                java.util.List.of(FieldDefinition.string("code", "Code")
                        .defaultValue("abc")
                        .validationRegex("[A-Z]+"))
        )))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("defaultValue");
        assertThatThrownBy(() -> validator.validateEntity(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                java.util.List.of(FieldDefinition.bool("enabled", "Enabled").defaultValue("abc"))
        )))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("boolean defaultValue");
    }

    @Test
    void shouldRequireJsonFieldForMultipleDictionaryBinding() {
        ModuleDefinitionValidator validator = new ModuleDefinitionValidator();

        assertThatThrownBy(() -> validator.validateEntity(new EntityDefinition(
                "customer",
                "crm_customer",
                "Customer",
                java.util.List.of(FieldDefinition.string("tags", "Tags")
                        .dictionary("crm", "customer_tag", OptionSelectionMode.MULTIPLE))
        )))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("multiple dictionary binding requires JSON field");
    }

    @Test
    void shouldValidateZonedTimestampCompanionFieldContract() {
        ModuleDefinitionValidator validator = new ModuleDefinitionValidator();

        validator.validateEntity(new EntityDefinition(
                "meeting",
                "app_meeting",
                "Meeting",
                java.util.List.of(
                        FieldDefinition.zonedTimestamp("meetingAt", "Meeting At").column("meeting_at").required(),
                        FieldDefinition.zonedTimestampTimeZone("meetingAt", "meeting_at").required()
                )
        ));

        assertThatThrownBy(() -> validator.validateEntity(new EntityDefinition(
                "meeting",
                "app_meeting",
                "Meeting",
                java.util.List.of(FieldDefinition.zonedTimestamp("meetingAt", "Meeting At").column("meeting_at"))
        )))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("zoned timestamp requires timeZone field");
        assertThatThrownBy(() -> validator.validateEntity(new EntityDefinition(
                "meeting",
                "app_meeting",
                "Meeting",
                java.util.List.of(
                        FieldDefinition.zonedTimestamp("meetingAt", "Meeting At").column("meeting_at"),
                        FieldDefinition.integer("meetingAtTimeZone", "Time Zone").column("meeting_at_timezone")
                )
        )))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("zoned timestamp timeZone field must be STRING");
        assertThatThrownBy(() -> validator.validateEntity(new EntityDefinition(
                "meeting",
                "app_meeting",
                "Meeting",
                java.util.List.of(
                        FieldDefinition.zonedTimestamp("meetingAt", "Meeting At").column("meeting_at"),
                        FieldDefinition.string("meetingAtTimeZone", "Time Zone").column("time_zone")
                )
        )))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("zoned timestamp timeZone column mismatch");
        assertThatThrownBy(() -> validator.validateEntity(new EntityDefinition(
                "meeting",
                "app_meeting",
                "Meeting",
                java.util.List.of(
                        FieldDefinition.zonedTimestamp("meetingAt", "Meeting At").column("meeting_at").required(),
                        FieldDefinition.zonedTimestampTimeZone("meetingAt", "meeting_at")
                )
        )))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("required zoned timestamp requires required timeZone field");
    }
}
