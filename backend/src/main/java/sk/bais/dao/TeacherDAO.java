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

import sk.bais.model.Teacher;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre ucitelov (rola TEACHER).
 */
public class TeacherDAO {

    private static final Logger log = LoggerFactory.getLogger(TeacherDAO.class);

    private static final String SQL_LIST =
            "SELECT u.id, u.email, u.first_name, u.last_name, u.password_hash, " +
            "       u.is_active, u.profile_picture_url, u.created_at, u.updated_at " +
            "FROM \"user\" u " +
            "JOIN user_role ur ON ur.user_id = u.id " +
            "JOIN role r ON r.id = ur.role_id " +
            "WHERE r.name = 'TEACHER' " +
            "ORDER BY u.id";

    private static final String SQL_GET_BY_ID =
            "SELECT u.id, u.email, u.first_name, u.last_name, u.password_hash, " +
            "       u.is_active, u.profile_picture_url, u.created_at, u.updated_at " +
            "FROM \"user\" u " +
            "JOIN user_role ur ON ur.user_id = u.id " +
            "JOIN role r ON r.id = ur.role_id " +
            "WHERE r.name = 'TEACHER' AND u.id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO \"user\" (email, first_name, last_name, password_hash, is_active, profile_picture_url) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_ROLE =
            "INSERT INTO user_role (user_id, role_id) " +
            "SELECT ?, id FROM role WHERE name = 'TEACHER'";

    private static final String SQL_UPDATE =
            "UPDATE \"user\" SET email = ?, first_name = ?, last_name = ?, " +
            "is_active = ?, profile_picture_url = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM \"user\" WHERE id = ?";

    // LIST
    public List<Teacher> list() throws SQLException {
        List<Teacher> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} teacher zaznamov", list.size());
        return list;
    }

    // GET BY ID
    public Optional<Teacher> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdeny teacher id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("Teacher id={} nenajdeny", id);
        return Optional.empty();
    }

    // CREATE
    public Teacher create(Teacher teacher) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, teacher.getEmail());
                    stmt.setString(2, teacher.getFirstName());
                    stmt.setString(3, teacher.getLastName());
                    stmt.setString(4, teacher.getPasswordHash());
                    stmt.setBoolean(5, teacher.isActive());
                    stmt.setString(6, teacher.getProfilePictureUrl());
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) teacher.setId(keys.getInt(1));
                    }
                }
                try (PreparedStatement roleStmt = conn.prepareStatement(SQL_INSERT_ROLE)) {
                    roleStmt.setInt(1, teacher.getId());
                    roleStmt.executeUpdate();
                }
                conn.commit();
                log.info("Vytvoreny teacher id={}", teacher.getId());
            } catch (SQLException e) {
                conn.rollback();
                log.error("Chyba pri vytvarani zaznamu, rollback", e);
                throw e;
            }
        }
        return teacher;
    }

    // UPDATE
    public boolean update(Teacher teacher) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, teacher.getEmail());
            stmt.setString(2, teacher.getFirstName());
            stmt.setString(3, teacher.getLastName());
            stmt.setBoolean(4, teacher.isActive());
            stmt.setString(5, teacher.getProfilePictureUrl());
            stmt.setInt(6, teacher.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany teacher id={}", id);
            return deleted;
        }
    }

    // MAPPER
    private Teacher mapRow(ResultSet rs) throws SQLException {
        return new Teacher(
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