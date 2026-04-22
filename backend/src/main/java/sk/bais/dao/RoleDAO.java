package sk.bais.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.bais.model.Role;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku role.
 * Role su zvycajne seedovane a menene len adminmi.
 */
public class RoleDAO {

    private static final Logger log = LoggerFactory.getLogger(RoleDAO.class);

    private static final String COLS = "id, name, description, created_at";

    private static final String SQL_LIST =
            "SELECT " + COLS + " FROM role ORDER BY id";

    private static final String SQL_GET_BY_ID =
            "SELECT " + COLS + " FROM role WHERE id = ?";

    private static final String SQL_GET_BY_NAME =
            "SELECT " + COLS + " FROM role WHERE name = ?";

    private static final String SQL_GET_BY_USER =
            "SELECT r.id, r.name, r.description, r.created_at " +
            "FROM role r " +
            "JOIN user_role ur ON ur.role_id = r.id " +
            "WHERE ur.user_id = ? ORDER BY r.id";

    private static final String SQL_INSERT =
            "INSERT INTO role (name, description) VALUES (?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE role SET name = ?, description = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM role WHERE id = ?";

    /**
     * Zoznam vsetkych roli.
     */
    public List<Role> list() throws SQLException {
        List<Role> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} role zaznamov", list.size());
        return list;
    }

    /**
     * Najde rolu podla ID.
     */
    public Optional<Role> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Najde rolu podla nazvu (napr. 'STUDENT').
     */
    public Optional<Role> getByName(String name) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_NAME)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Zoznam roli priradenych danemu pouzivatelovi.
     */
    public List<Role> listByUser(int userId) throws SQLException {
        List<Role> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_USER)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} roli pre userId={}", list.size(), userId);
        return list;
    }

    /**
     * Vytvori novu rolu.
     */
    public Role create(Role r) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, r.getName());
            stmt.setString(2, r.getDescription());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) r.setId(keys.getInt(1));
            }
        }
        log.info("Vytvorena rola id={} name={}", r.getId(), r.getName());
        return r;
    }

    /**
     * Upravi existujucu rolu.
     */
    public boolean update(Role r) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, r.getName());
            stmt.setString(2, r.getDescription());
            stmt.setInt(3, r.getId());
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Rola id={} upravena", r.getId());
            return updated;
        }
    }

    /**
     * Vymaze rolu podla ID.
     * Pozor: DB RESTRICT brani vymazaniu ak maju pouzivatelia tuto rolu.
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazana rola id={}", id);
            return deleted;
        }
    }

    private Role mapRow(ResultSet rs) throws SQLException {
        Role r = new Role();
        r.setId(rs.getInt("id"));
        r.setName(rs.getString("name"));
        r.setDescription(rs.getString("description"));
        r.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return r;
    }
}
