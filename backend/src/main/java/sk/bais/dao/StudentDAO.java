package sk.bais.dao;

import sk.bais.model.Student;
import sk.bais.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO trieda pre studentov.
 * Studenti su uzivatelia s rolou STUDENT.
 */
public class StudentDAO {

    private static final String SQL_LIST =
            "SELECT u.id, u.email, u.first_name, u.last_name, u.password_hash, " +
            "       u.is_active, u.profile_picture_url, u.created_at, u.updated_at " +
            "FROM \"user\" u " +
            "JOIN user_role ur ON ur.user_id = u.id " +
            "JOIN role r ON r.id = ur.role_id " +
            "WHERE r.name = 'STUDENT' " +
            "ORDER BY u.id";

    private static final String SQL_GET_BY_ID =
            "SELECT u.id, u.email, u.first_name, u.last_name, u.password_hash, " +
            "       u.is_active, u.profile_picture_url, u.created_at, u.updated_at " +
            "FROM \"user\" u " +
            "JOIN user_role ur ON ur.user_id = u.id " +
            "JOIN role r ON r.id = ur.role_id " +
            "WHERE r.name = 'STUDENT' AND u.id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO \"user\" (email, first_name, last_name, password_hash, is_active, profile_picture_url) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_ROLE =
            "INSERT INTO user_role (user_id, role_id) " +
            "SELECT ?, id FROM role WHERE name = 'STUDENT'";

    private static final String SQL_UPDATE =
            "UPDATE \"user\" SET email = ?, first_name = ?, last_name = ?, " +
            "is_active = ?, profile_picture_url = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM \"user\" WHERE id = ?";

    // --- LIST ---

    public List<Student> list() throws SQLException {
        List<Student> students = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                students.add(mapRow(rs));
            }
        }
        return students;
    }

    // --- GET BY ID ---

    public Optional<Student> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    // --- CREATE ---
    // Vlozi uzivatela a priradi mu rolu STUDENT v jednej transakcii

    public Student create(Student student) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. vloz do "user"
                try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, student.getEmail());
                    stmt.setString(2, student.getFirstName());
                    stmt.setString(3, student.getLastName());
                    stmt.setString(4, student.getPasswordHash());
                    stmt.setBoolean(5, student.isActive());
                    stmt.setString(6, student.getProfilePictureUrl());
                    stmt.executeUpdate();

                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) student.setId(keys.getInt(1));
                    }
                }

                // 2. priradi rolu STUDENT
                try (PreparedStatement roleStmt = conn.prepareStatement(SQL_INSERT_ROLE)) {
                    roleStmt.setInt(1, student.getId());
                    roleStmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        return student;
    }

    // --- UPDATE ---

    public boolean update(Student student) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, student.getEmail());
            stmt.setString(2, student.getFirstName());
            stmt.setString(3, student.getLastName());
            stmt.setBoolean(4, student.isActive());
            stmt.setString(5, student.getProfilePictureUrl());
            stmt.setInt(6, student.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    // --- DELETE ---

    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // --- MAPPER ---

    private Student mapRow(ResultSet rs) throws SQLException {
        return new Student(
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