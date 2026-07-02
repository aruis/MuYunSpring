package net.ximatai.muyun.spring.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RoleKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeTitleEnumJacksonConfigurationTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new MuYunSpringJacksonConfiguration().codeTitleEnumJacksonModule());

    @Test
    void shouldSerializeCodeTitleEnumByCode() throws Exception {
        Role role = new Role();
        role.setRoleKind(RoleKind.DATA_GRANT);

        String json = objectMapper.writeValueAsString(role);

        assertThat(json).contains("\"roleKind\":\"dataGrant\"");
    }

    @Test
    void shouldDeserializeCodeTitleEnumByCode() throws Exception {
        Role role = objectMapper.readValue("{\"roleKind\":\"dataGrant\"}", Role.class);

        assertThat(role.getRoleKind()).isEqualTo(RoleKind.DATA_GRANT);
    }
}
