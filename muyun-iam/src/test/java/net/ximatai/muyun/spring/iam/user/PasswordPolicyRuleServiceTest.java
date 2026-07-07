package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordPolicyRuleServiceTest {

    @Test
    void shouldFallbackToDefaultMinLengthRuleWhenNoRuleConfigured() {
        PasswordPolicyRuleDao dao = mock(PasswordPolicyRuleDao.class);
        when(dao.query(any(Criteria.class), any(PageRequest.class), any(Sort[].class))).thenReturn(List.of());
        PasswordPolicyRuleService service = new PasswordPolicyRuleService(dao);

        service.validatePassword("secret1");

        assertThatThrownBy(() -> service.validatePassword("12345"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("密码长度不能少于 6 位");
    }

    @Test
    void shouldKeepInitialDataIdsWithinStandardEntityLength() {
        PasswordPolicyRuleDao dao = mock(PasswordPolicyRuleDao.class);
        PasswordPolicyRuleService service = new PasswordPolicyRuleService(dao);

        assertThat(service.initialData())
                .extracting(PasswordPolicyRule::getId)
                .allSatisfy(id -> assertThat(id.length()).isLessThanOrEqualTo(PlatformAbilityFields.TREE_PARENT_LENGTH));
    }

    @Test
    void shouldValidateEnabledRulesByRegexAndMessage() {
        PasswordPolicyRuleDao dao = mock(PasswordPolicyRuleDao.class);
        when(dao.query(any(Criteria.class), any(PageRequest.class), any(Sort[].class))).thenReturn(List.of(
                rule("contains_digit", "^.*\\d.*$", "密码必须包含数字", 20)
        ));
        PasswordPolicyRuleService service = new PasswordPolicyRuleService(dao);

        service.validatePassword("secret1");

        assertThatThrownBy(() -> service.validatePassword("secret"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("密码必须包含数字");
    }

    @Test
    void shouldNormalizeGlobalRuleAndRejectInvalidRegex() {
        PasswordPolicyRuleDao dao = mock(PasswordPolicyRuleDao.class);
        PasswordPolicyRuleService service = new PasswordPolicyRuleService(dao);
        PasswordPolicyRule rule = new PasswordPolicyRule();
        rule.setRuleCode(" contains_upper ");
        rule.setPattern("^.*[A-Z].*$");
        rule.setMessage(" 密码必须包含大写字母 ");

        service.beforeInsert(rule);

        assertThat(rule.getScopeType()).isEqualTo(PasswordPolicyScopeType.GLOBAL);
        assertThat(rule.getScopeId()).isNull();
        assertThat(rule.getScopeKey()).isEqualTo("global:");
        assertThat(rule.getRuleCode()).isEqualTo("contains_upper");
        assertThat(rule.getTitle()).isEqualTo("contains_upper");
        assertThat(rule.getMessage()).isEqualTo("密码必须包含大写字母");

        rule.setPattern("[");
        assertThatThrownBy(() -> service.beforeUpdate(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("invalid password policy regex");
    }

    private PasswordPolicyRule rule(String code, String pattern, String message, int sortOrder) {
        PasswordPolicyRule rule = new PasswordPolicyRule();
        rule.setScopeType(PasswordPolicyScopeType.GLOBAL);
        rule.setScopeKey("global:");
        rule.setRuleCode(code);
        rule.setTitle(code);
        rule.setPattern(pattern);
        rule.setMessage(message);
        rule.setEnabled(Boolean.TRUE);
        rule.setSortOrder(sortOrder);
        return rule;
    }
}
