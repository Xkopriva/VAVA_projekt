package sk.bais.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.bais.model.Admin;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre adminov (rola ADMIN).
 * Logovanie: INFO pre úspešné operácie, ERROR pre SQL výnimky.
 */
public class AdminDAO {

    private static final Logger log = LoggerFactory.getLogger(AdminDAO.class);

    private static final String SQL_LIST =
            "SELECT u.id, u.email, u.first_name, u.last_name, u.password_hash, " +
            "       u.is_active, u.profile_picture_url, u.created_at, u.updated_at " +
            "FROM \"user\" u " +
            "JOIN user_role ur ON ur.user_id = u.id " +
            "JOIN role r ON r.id = ur.role_id " +
            "WHERE r.name = 'ADMIN' ORDER BY u.id";

    private static final String SQL_GET_BY_ID =
            "SELECT u.id, u.email, u.first_name, u.last_name, u.password_hash, " +
            "       u.is_active, u.profile_picture_url, u.created_at, u.updated_at " +
            "FROM \"user\" u WHERE u.id = ?";

    private static final String SQL_UPDATE =
            "UPDATE \"user\" SET email = ?, first_name = ?, last_name = ?, is_active = ?, profile_picture_url = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE = "DELETE FROM \"user\" WHERE id = ?";

    //  LIST 
    public List<Admin> list() throws SQLException {
        List<Admin> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        log.info("Nacitanych {} admin zaznamov", list.size());
        return list;
    }

    //  GET BY ID 
    public Optional<Admin> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdeny admin id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("Admin id={} nenajdeny", id);
        return Optional.empty();
    }

    //  UPDATE 
    public boolean update(Admin admin) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, admin.getEmail());
            stmt.setString(2, admin.getFirstName());
            stmt.setString(3, admin.getLastName());
            stmt.setBoolean(4, admin.isActive());
            stmt.setString(5, admin.getProfilePictureUrl());
            stmt.setInt(6, admin.getId());
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Admin id={} uspesne upraveny", admin.getId());
            return updated;
        }
    }

    //  DELETE 
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany admin id={}", id);
            return deleted;
        }
    }

    //  MAPPER 
    private Admin mapRow(ResultSet rs) throws SQLException {
        return new Admin(
                rs.getInt("id"),
                rs.getString("email"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("password_hash"),
                rs.getBoolean("is_active"),
                rs.getString("profile_picture_url"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }
}