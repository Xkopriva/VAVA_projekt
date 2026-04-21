package sk.bais.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.bais.model.UserRole;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku user_role.
 * Kompozitny PK: (user_id, role_id) — spravuje priradenie roli k pouzivatelom.
 */
public class UserRoleDAO {

    private static final Logger log = LoggerFactory.getLogger(UserRoleDAO.class);

    private static final String SQL_LIST_BY_USER =
            "SELECT user_id, role_id, assigned_at, assigned_by FROM user_role WHERE user_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO user_role (user_id, role_id, assigned_by) VALUES (?, ?, ?)";

    private static final String SQL_DELETE =
            "DELETE FROM user_role WHERE user_id = ? AND role_id = ?";

    private static final String SQL_DELETE_ALL_BY_USER =
            "DELETE FROM user_role WHERE user_id = ?";

    private static final String SQL_EXISTS =
            "SELECT 1 FROM user_role WHERE user_id = ? AND role_id = ?";

    private static final String SQL_EXISTS_BY_ROLE_NAME =
            "SELECT 1 FROM user_role ur " +
            "JOIN role r ON r.id = ur.role_id " +
            "WHERE ur.user_id = ? AND r.name = ?";

    /**
     * Zoznam vsetkych priradeni roli pre daneho pouzivatela.
     */
    public List<UserRole> listByUser(int userId) throws SQLException {
        List<UserRole> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_USER)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} roli pre userId={}", list.size(), userId);
        return list;
    }

    /**
     * Priradi rolu pouzivatelovi.
     *
     * @param userId     ID pouzivatela
     * @param roleId     ID roly
     * @param assignedBy ID admina ktory priradzuje (null = system)
     */
    public void assign(int userId, int roleId, Integer assignedBy) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, roleId);
            if (assignedBy != null) stmt.setInt(3, assignedBy);
            else stmt.setNull(3, Types.INTEGER);
            stmt.executeUpdate();
        }
        log.info("Rola roleId={} priradena userId={} adminId={}", roleId, userId, assignedBy);
    }

    /**
     * Odobre rolu pouzivatelovi.
     */
    public boolean revoke(int userId, int roleId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, roleId);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Rola roleId={} odobrata userId={}", roleId, userId);
            return deleted;
        }
    }

    /**
     * Odobre vsetky role pouzivatelovi.
     */
    public int revokeAll(int userId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL_BY_USER)) {
            stmt.setInt(1, userId);
            int count = stmt.executeUpdate();
            log.info("Odobranych {} roli pre userId={}", count, userId);
            return count;
        }
    }

    /**
     * Skontroluje ci ma pouzivatel danu rolu (podla ID roly).
     */
    public boolean hasRole(int userId, int roleId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_EXISTS)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Skontroluje ci ma pouzivatel danu rolu (podla nazvu, napr. 'ADMIN').
     */
    public boolean hasRoleByName(int userId, String roleName) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_EXISTS_BY_ROLE_NAME)) {
            stmt.setInt(1, userId);
            stmt.setString(2, roleName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private UserRole mapRow(ResultSet rs) throws SQLException {
        UserRole ur = new UserRole();
        ur.setUserId(rs.getInt("user_id"));
        ur.setRoleId(rs.getInt("role_id"));
        ur.setAssignedAt(rs.getObject("assigned_at", java.time.OffsetDateTime.class));
        int ab = rs.getInt("assigned_by");
        if (!rs.wasNull()) ur.setAssignedBy(ab);
        return ur;
    }
}
