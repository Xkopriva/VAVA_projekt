package sk.bais.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.bais.model.RolePermission;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku role_permission.
 * Kompozitny PK: (role_id, permission_id) — spravuje priradenie opravneni k rolam.
 */
public class RolePermissionDAO {

    private static final Logger log = LoggerFactory.getLogger(RolePermissionDAO.class);

    private static final String SQL_LIST_BY_ROLE =
            "SELECT role_id, permission_id, granted_at FROM role_permission WHERE role_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO role_permission (role_id, permission_id) VALUES (?, ?)";

    private static final String SQL_DELETE =
            "DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?";

    private static final String SQL_DELETE_ALL_BY_ROLE =
            "DELETE FROM role_permission WHERE role_id = ?";

    private static final String SQL_EXISTS =
            "SELECT 1 FROM role_permission WHERE role_id = ? AND permission_id = ?";

    /**
     * Zoznam vsetkych priradeni opravneni pre danu rolu.
     */
    public List<RolePermission> listByRole(int roleId) throws SQLException {
        List<RolePermission> list = new ArrayList<>();
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
     * Priradi opravnenie k roli.
     */
    public void grant(int roleId, int permissionId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            stmt.setInt(1, roleId);
            stmt.setInt(2, permissionId);
            stmt.executeUpdate();
        }
        log.info("Opravnenie permissionId={} pridelene roleId={}", permissionId, roleId);
    }

    /**
     * Odobre opravnenie z role.
     */
    public boolean revoke(int roleId, int permissionId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, roleId);
            stmt.setInt(2, permissionId);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Opravnenie permissionId={} odobrate roleId={}", permissionId, roleId);
            return deleted;
        }
    }

    /**
     * Odobre vsetky opravnenia z role.
     */
    public int revokeAll(int roleId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL_BY_ROLE)) {
            stmt.setInt(1, roleId);
            int count = stmt.executeUpdate();
            log.info("Odobratych {} opravneni pre roleId={}", count, roleId);
            return count;
        }
    }

    /**
     * Skontroluje ci rola ma dane opravnenie.
     */
    public boolean hasPermission(int roleId, int permissionId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_EXISTS)) {
            stmt.setInt(1, roleId);
            stmt.setInt(2, permissionId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private RolePermission mapRow(ResultSet rs) throws SQLException {
        return new RolePermission(
                rs.getInt("role_id"),
                rs.getInt("permission_id"),
                rs.getObject("granted_at", java.time.OffsetDateTime.class)
        );
    }
}
