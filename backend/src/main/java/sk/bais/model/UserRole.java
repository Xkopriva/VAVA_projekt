package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku user_role.
 * Kompozitny PK: (user_id, role_id) — junction medzi user a role.
 * Pouzivatelia mozu mat viacero roli.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    private int userId;             // FK -> user
    private int roleId;             // FK -> role
    private OffsetDateTime assignedAt;
    private Integer assignedBy;     // FK -> user nullable (null = system/self-registered)
}
