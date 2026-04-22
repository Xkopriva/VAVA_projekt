package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku role_permission.
 * Kompozitny PK: (role_id, permission_id) — junction medzi role a permission.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    private int roleId;             // FK -> role
    private int permissionId;       // FK -> permission
    private OffsetDateTime grantedAt;
}
