package sk.bais.dao;

import sk.bais.model.Enrollment;
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
 * DAO trieda pre tabulku enrollment.
 */
public class EnrollmentDAO {

    private static final String SQL_LIST =
            "SELECT id, student_id, subject_id, semester_id, " +
            "       attempt_number, enrolled_at, status " +
            "FROM enrollment ORDER BY id";

    private static final String SQL_LIST_BY_STUDENT =
            "SELECT id, student_id, subject_id, semester_id, " +
            "       attempt_number, enrolled_at, status " +
            "FROM enrollment WHERE student_id = ? ORDER BY enrolled_at DESC";

    private static final String SQL_GET_BY_ID =
            "SELECT id, student_id, subject_id, semester_id, " +
            "       attempt_number, enrolled_at, status " +
            "FROM enrollment WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO enrollment (student_id, subject_id, semester_id, attempt_number, status) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE enrollment SET status = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM enrollment WHERE id = ?";

    // --- LIST vsetkych ---

    public List<Enrollment> list() throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // --- LIST podla studenta --- (najcastejsi use-case)

    public List<Enrollment> listByStudent(int studentId) throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_STUDENT)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // --- GET BY ID ---

    public Optional<Enrollment> getById(int id) throws SQLException {
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

    public Enrollment create(Enrollment e) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, e.getStudentId());
            stmt.setInt(2, e.getSubjectId());
            stmt.setInt(3, e.getSemesterId());
            stmt.setInt(4, e.getAttemptNumber());
            stmt.setString(5, e.getStatus().name());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) e.setId(keys.getInt(1));
            }
        }
        return e;
    }

    // --- UPDATE STATUS --- (najcastejsia zmena — ACTIVE -> PASSED/FAILED/WITHDRAWN)

    public boolean updateStatus(int enrollmentId, Enrollment.Status newStatus) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, enrollmentId);
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

    private Enrollment mapRow(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setId(rs.getInt("id"));
        e.setStudentId(rs.getInt("student_id"));
        e.setSubjectId(rs.getInt("subject_id"));
        e.setSemesterId(rs.getInt("semester_id"));
        e.setAttemptNumber(rs.getInt("attempt_number"));
        e.setEnrolledAt(rs.getObject("enrolled_at", java.time.OffsetDateTime.class));
        String status = rs.getString("status");
        if (status != null) e.setStatus(Enrollment.Status.valueOf(status));
        return e;
    }
}