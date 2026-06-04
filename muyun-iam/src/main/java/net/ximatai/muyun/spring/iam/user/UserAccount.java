package net.ximatai.muyun.spring.iam.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardDataScopedEnabledSortableEntity;

@Getter
@Setter
@Table(name = "iam_user", comment = "User account")
@CompositeIndex(columns = {"tenant_id", "username"}, unique = true)
public class UserAccount extends StandardDataScopedEnabledSortableEntity {
    @Column(name = "username", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Username")
    private String username;

    @Column(name = "mobile", type = ColumnType.VARCHAR, length = 32, comment = "Mobile")
    private String mobile;

    @Column(name = "email", type = ColumnType.VARCHAR, length = 128, comment = "Email")
    private String email;

    @Column(name = "organization_id", type = ColumnType.VARCHAR, length = 32, comment = "Organization id")
    private String organizationId;

    @Column(name = "password_hash", type = ColumnType.VARCHAR, length = 256, nullable = false,
            comment = "Password hash")
    @JsonIgnore
    private String passwordHash;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private transient String password;
}
