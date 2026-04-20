package sk.bais.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.bais.model.Enrollment;
import sk.bais.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO trieda pre tabulku enrollment.
 * Logovanie: INFO pre uspesne operacie, ERROR pre SQL vynimky.
 */
public class EnrollmentDAO {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentDAO.class);

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

    // --- LIST ---

    public List<Enrollment> list() throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} enrollment zaznamov", list.size());
        return list;
    }

    // --- LIST BY STUDENT ---

    public List<Enrollment> listByStudent(int studentId) throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_STUDENT)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} zapisov pre studentId={}", list.size(), studentId);
        return list;
    }

    // --- GET BY ID ---

    public Optional<Enrollment> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdeny enrollment id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("Enrollment id={} nenajdeny", id);
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
        log.info("Vytvoreny enrollment id={} pre studentId={} subjectId={}",
                e.getId(), e.getStudentId(), e.getSubjectId());
        return e;
    }

    // --- UPDATE STATUS ---

    public boolean updateStatus(int enrollmentId, Enrollment.Status newStatus) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, enrollmentId);
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Enrollment id={} -> status={}", enrollmentId, newStatus);
            return updated;
        }
    }

    // --- DELETE ---

    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany enrollment id={}", id);
            return deleted;
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