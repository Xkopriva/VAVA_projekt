package sk.bais.auth;

import lombok.Getter;

import java.util.Set;

/**
 * AuthContext drží informácie o aktuálne prihlásenom užívateľovi.
 *
 * Vytvára sa po úspešnom prihlásení v AuthService a predáva sa
 * do každej Service metódy — namiesto toho aby sme v DB stále
 * dotazovali "kto je prihlásený".
 *
 * Nemutabilný — po vytvorení sa nemení.
 */
@Getter  // Lombok: generuje gettery pre všetky polia
public class AuthContext {

    private final int userId;
    private final String email;
    private final String role;             // napr. "STUDENT", "TEACHER", "ADMIN"
    private final Set<String> permissions; // napr. {"subjects:read", "enrollments:write"}

    public AuthContext(int userId, String email, String role, Set<String> permissions) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.permissions = Set.copyOf(permissions); // nemutabilná kópia
    }

    /**
     * Skontroluje či má prihlásený užívateľ dané oprávnenie.
     * Volá sa v každej Service metóde pred vykonaním akcie.
     *
     * Príklad: ctx.hasPermission("enrollments:write")
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /**
     * Skratka pre kontrolu roly — používa sa keď nestačí jedno oprávnenie.
     * Príklad: ctx.hasRole("ADMIN")
     */
    public boolean hasRole(String roleName) {
        return this.role.equalsIgnoreCase(roleName);
    }

    @Override
    public String toString() {
        return "AuthContext{userId=" + userId + ", email='" + email + "', role='" + role + "'}";
    }
}
