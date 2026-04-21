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

import sk.bais.model.Permission;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku permission.
 * Opravnenia su zvycajne seedovane a spravovane adminmi.
 */
public class PermissionDAO {

    private static final Logger log = LoggerFactory.getLogger(PermissionDAO.class);

    private static final String COLS = "id, name, resource, action";

    private static final String SQL_LIST =
            "SELECT " + COLS + " FROM permission ORDER BY resource, action";

    private static final String SQL_GET_BY_ID =
            "SELECT " + COLS + " FROM permission WHERE id = ?";

    private static final String SQL_GET_BY_NAME =
            "SELECT " + COLS + " FROM permission WHERE name = ?";

    private static final String SQL_LIST_BY_ROLE =
            "SELECT p.id, p.name, p.resource, p.action " +
            "FROM permission p " +
            "JOIN role_permission rp ON rp.permission_id = p.id " +
            "WHERE rp.role_id = ? ORDER BY p.resource, p.action";

    private static final String SQL_LIST_BY_USER =
            "SELECT DISTINCT p.id, p.name, p.resource, p.action " +
            "FROM permission p " +
            "JOIN role_permission rp ON rp.permission_id = p.id " +
            "JOIN user_role ur ON ur.role_id = rp.role_id " +
            "WHERE ur.user_id = ? ORDER BY p.resource, p.action";

    private static final String SQL_INSERT =
            "INSERT INTO permission (name, resource, action) VALUES (?, ?, ?)";

    private static final String SQL_DELETE =
            "DELETE FROM permission WHERE id = ?";

    /**
     * Zoznam vsetkych opravneni.
     */
    public List<Permission> list() throws SQLException {
        List<Permission> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} permission zaznamov", list.size());
        return list;
    }

    /**
     * Najde opravnenie podla ID.
     */
    public Optional<Permission> getById(int id) throws SQLException {
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
     * Najde opravnenie podla nazvu (napr. 'subjects:write').
     */
    public Optional<Permission> getByName(String name) throws SQLException {
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
     * Zoznam opravneni priradenych danej roli.
     */
    public List<Permission> listByRole(int roleId) throws SQLException {
        List<Permission> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_ROLE)) {
            stmt.setInt(1, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} opravneni pre roleId={}", list.size(), roleId);
        return list;
    }

    /**
     * Zoznam vsetkych opravneni daneho pouzivatela (cez vsetky jeho role).
     * Pouzitelne pre autorizacne kontroly.
     */
    public List<Permission> listByUser(int userId) throws SQLException {
        List<Permission> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_USER)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} opravneni pre userId={}", list.size(), userId);
        return list;
    }

    /**
     * Vytvori nove opravnenie.
     */
    public Permission create(Permission p) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, p.getName());
            stmt.setString(2, p.getResource());
            stmt.setString(3, p.getAction());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
        }
        log.info("Vytvorene opravnenie id={} name={}", p.getId(), p.getName());
        return p;
    }

    /**
     * Vymaze opravnenie podla ID.
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazane opravnenie id={}", id);
            return deleted;
        }
    }

    private Permission mapRow(ResultSet rs) throws SQLException {
        return new Permission(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("resource"),
                rs.getString("action")
        );
    }
}
