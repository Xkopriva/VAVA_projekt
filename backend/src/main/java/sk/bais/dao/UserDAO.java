package sk.bais.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.bais.model.User;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku "user".
 * Pracuje so vsetkymi pouzivatelmi bez ohladu na rolu.
 * Pre role-specificke operacie pouzi StudentDAO / TeacherDAO / AdminDAO / PowerUserDAO.
 */
public class UserDAO {

    private static final Logger log = LoggerFactory.getLogger(UserDAO.class);

    private static final String COLS =
            "id, email, first_name, last_name, password_hash, is_active, " +
            "profile_picture_url, created_at, updated_at";

    private static final String SQL_LIST =
            "SELECT " + COLS + " FROM \"user\" ORDER BY last_name, first_name";

    private static final String SQL_GET_BY_ID =
            "SELECT " + COLS + " FROM \"user\" WHERE id = ?";

    private static final String SQL_GET_BY_EMAIL =
            "SELECT " + COLS + " FROM \"user\" WHERE email = ?";

    private static final String SQL_INSERT =
            "INSERT INTO \"user\" (email, first_name, last_name, password_hash, is_active, profile_picture_url) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE \"user\" SET email = ?, first_name = ?, last_name = ?, " +
            "is_active = ?, profile_picture_url = ? WHERE id = ?";

    private static final String SQL_UPDATE_PASSWORD =
            "UPDATE \"user\" SET password_hash = ? WHERE id = ?";

    private static final String SQL_SET_ACTIVE =
            "UPDATE \"user\" SET is_active = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM \"user\" WHERE id = ?";
            
    private static final String SQL_ASSIGN_ROLE = "INSERT INTO user_role (user_id, role_id) " +
            "SELECT ?, id FROM role WHERE name = ?";
    /**
     * Zoznam vsetkych pouzivatelov.
     */
    public List<User> list() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} user zaznamov", list.size());
        return list;
    }

    /**
     * Najde pouzivatela podla ID.
     */
    public Optional<User> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdeny user id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("User id={} nenajdeny", id);
        return Optional.empty();
    }

    /**
     * Najde pouzivatela podla emailu (pre prihlasenie).
     */
    public Optional<User> getByEmail(String email) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_EMAIL)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Vytvori noveho pouzivatela.
     */
    public User create(User u) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, u.getEmail());
            stmt.setString(2, u.getFirstName());
            stmt.setString(3, u.getLastName());
            stmt.setString(4, u.getPasswordHash());
            stmt.setBoolean(5, u.isActive());
            stmt.setString(6, u.getProfilePictureUrl());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) u.setId(keys.getInt(1));
            }
        }
        log.info("Vytvoreny user id={} email={}", u.getId(), u.getEmail());
        return u;
    }

    /**
     * Upravi zakladne udaje pouzivatela (bez hesla).
     */
    public boolean update(User u) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, u.getEmail());
            stmt.setString(2, u.getFirstName());
            stmt.setString(3, u.getLastName());
            stmt.setBoolean(4, u.isActive());
            stmt.setString(5, u.getProfilePictureUrl());
            stmt.setInt(6, u.getId());

            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("User id={} uspesne upraveny", u.getId());
            return updated;
        }
    }

    /**
     * Zmeni heslo pouzivatela (prijima uz hashované heslo).
     */
    public boolean updatePassword(int userId, String newPasswordHash) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, userId);
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Heslo pre user id={} zmenene", userId);
            return updated;
        }
    }

    /**
     * Aktivuje alebo deaktivuje konto pouzivatela (soft-disable).
     */
    public boolean setActive(int userId, boolean active) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SET_ACTIVE)) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, userId);
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("User id={} -> is_active={}", userId, active);
            return updated;
        }
    }

    /**
     * Vymaze pouzivatela podla ID.
     * Pozor: DB RESTRICT brani vymazaniu ak su viazane zaznamy (enrollments, marks atd.).
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany user id={}", id);
            return deleted;
        }
    }

    /**
     * Prida uyivatelovi rolu
     */
    public void assignRole(int userId, String roleName) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(SQL_ASSIGN_ROLE)) {
            stmt.setInt(1, userId);
            stmt.setString(2, roleName);
            stmt.executeUpdate();
            log.info("Rola {} priradena pouzivatelovi id={}", roleName, userId);
        }
    }

    /**
     * Mapovanie ResultSet na objekt User.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setActive(rs.getBoolean("is_active"));
        u.setProfilePictureUrl(rs.getString("profile_picture_url"));
        u.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        u.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return u;
    }
}
