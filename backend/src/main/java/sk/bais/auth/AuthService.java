package sk.bais.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.bais.util.DatabaseConnection;

/**
 * AuthService rieši dve veci:
 *
 * 1. AUTENTIFIKÁCIA — overí email + heslo voči DB
 * 2. AUTORIZÁCIA — načíta oprávnenia užívateľa z RBAC tabuliek
 *
 * TODO: Heslá sú v DB hashované (bcrypt/argon2).
 * Tu používame jednoduchú kontrolu hash == hash pre demonštráciu.
 * V produkcii treba použiť BCrypt.checkpw() alebo podobné.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Načíta usera + jeho rolu naraz jedným JOINom
    private static final String SQL_LOGIN =
            "SELECT u.id, u.email, u.password_hash, u.is_active, r.name AS role_name " +
            "FROM \"user\" u " +
            "JOIN user_role ur ON ur.user_id = u.id " +
            "JOIN role r ON r.id = ur.role_id " +
            "WHERE u.email = ? " +
            "LIMIT 1";

    // Načíta všetky oprávnenia pre danú rolu z RBAC tabuliek
    private static final String SQL_PERMISSIONS =
            "SELECT p.name " +
            "FROM permission p " +
            "JOIN role_permission rp ON rp.permission_id = p.id " +
            "JOIN role r ON r.id = rp.role_id " +
            "WHERE r.name = ?";

    /**
     * Pokus o prihlásenie.
     *
     * @param email    email užívateľa
     * @param password heslo v plaintexte (porovná sa s hashom z DB)
     * @return AuthContext ak prihlásenie prebehlo úspešne, inak empty
     */
    public Optional<AuthContext> login(String email, String password) {
        log.info("Pokus o prihlásenie: email={}", email);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LOGIN)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    log.warn("Prihlásenie zlyhalo — email nenájdený: {}", email);
                    return Optional.empty();
                }

                boolean isActive = rs.getBoolean("is_active");
                if (!isActive) {
                    log.warn("Prihlásenie zlyhalo — účet deaktivovaný: {}", email);
                    return Optional.empty();
                }

                String storedHash = rs.getString("password_hash");

                // TODO: nahradiť BCrypt.checkpw(password, storedHash) keď pridáme bcrypt závislosť
                // Teraz porovnávame priamo hash == zadaný reťazec (len pre vývoj!)
                if (!storedHash.equals(password)) {
                    log.warn("Prihlásenie zlyhalo — nesprávne heslo: {}", email);
                    return Optional.empty();
                }

                int userId = rs.getInt("id");
                String role = rs.getString("role_name");

                // Načítame oprávnenia z RBAC tabuliek
                Set<String> permissions = loadPermissions(conn, role);

                AuthContext ctx = new AuthContext(userId, email, role, permissions);
                log.info("Úspešné prihlásenie: userId={}, role={}, permissions={}",
                        userId, role, permissions.size());
                return Optional.of(ctx);
            }

        } catch (SQLException e) {
            log.error("Chyba pri prihlásení pre email={}", email, e);
            return Optional.empty();
        }
    }

    /**
     * Načíta sadu oprávnení pre danú rolu z DB.
     * Volá sa interne po overení hesla.
     */
    private Set<String> loadPermissions(Connection conn, String roleName) throws SQLException {
        Set<String> permissions = new HashSet<>();

        try (PreparedStatement stmt = conn.prepareStatement(SQL_PERMISSIONS)) {
            stmt.setString(1, roleName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(rs.getString("name"));
                }
            }
        }

        log.debug("Načítaných {} oprávnení pre rolu {}", permissions.size(), roleName);
        return permissions;
    }
}
