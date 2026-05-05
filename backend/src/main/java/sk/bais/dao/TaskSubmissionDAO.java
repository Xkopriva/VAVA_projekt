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

import sk.bais.model.TaskSubmission;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku task_submission.
 * Spravuje odovzdania zadani studentmi.
 */
public class TaskSubmissionDAO {

    private static final Logger log = LoggerFactory.getLogger(TaskSubmissionDAO.class);

    private static final String COLS =
            "id, task_id, student_id, submitted_at, content, file_url, " +
            "status, graded_by, graded_at";

    private static final String SQL_LIST_BY_TASK =
            "SELECT " + COLS + " FROM task_submission WHERE task_id = ? ORDER BY submitted_at NULLS LAST";

    private static final String SQL_LIST_BY_STUDENT =
            "SELECT " + COLS + " FROM task_submission WHERE student_id = ? ORDER BY submitted_at DESC NULLS LAST";

    private static final String SQL_GET_BY_ID =
            "SELECT " + COLS + " FROM task_submission WHERE id = ?";

    private static final String SQL_GET_BY_TASK_AND_STUDENT =
            "SELECT " + COLS + " FROM task_submission WHERE task_id = ? AND student_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO task_submission (task_id, student_id, submitted_at, content, file_url, status) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE task_submission SET submitted_at = ?, content = ?, file_url = ?, " +
            "status = ?, graded_by = ?, graded_at = ? " +
            "WHERE id = ?";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE task_submission SET status = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM task_submission WHERE id = ?";

    /**
     * Zoznam odovzdani pre dane zadanie.
     */
    public List<TaskSubmission> listByTask(int taskId) throws SQLException {
        List<TaskSubmission> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_TASK)) {
            stmt.setInt(1, taskId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} odovzdani pre taskId={}", list.size(), taskId);
        return list;
    }

    /**
     * Zoznam odovzdani pre daneho studenta.
     */
    public List<TaskSubmission> listByStudent(int studentId) throws SQLException {
        List<TaskSubmission> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_STUDENT)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} odovzdani pre studentId={}", list.size(), studentId);
        return list;
    }

    /**
     * Najde odovzdanie podla ID.
     */
    public Optional<TaskSubmission> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdene task_submission id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("TaskSubmission id={} nenajdene", id);
        return Optional.empty();
    }

    /**
     * Najde odovzdanie podla zadania a studenta (unique constraint).
     */
    public Optional<TaskSubmission> getByTaskAndStudent(int taskId, int studentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_TASK_AND_STUDENT)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Vloží alebo aktualizuje odovzdanie 
     */
    public TaskSubmission updateOrInsert(TaskSubmission ts) throws SQLException {
        Optional<TaskSubmission> existing = getByTaskAndStudent(ts.getTaskId(), ts.getStudentId());
        if (existing.isPresent()) {
            ts.setId(existing.get().getId());
            update(ts);
            return ts;
        } else {
            return create(ts);
        }
    }

    /**
     * Vytvori nove odovzdanie.
     */
    public TaskSubmission create(TaskSubmission ts) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, ts.getTaskId());
            stmt.setInt(2, ts.getStudentId());
            if (ts.getSubmittedAt() != null)
                stmt.setObject(3, ts.getSubmittedAt());
            else
                stmt.setNull(3, Types.TIMESTAMP_WITH_TIMEZONE);
            stmt.setString(4, ts.getContent());
            stmt.setString(5, ts.getFileUrl());
            stmt.setString(6, ts.getStatus().name());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) ts.setId(keys.getInt(1));
            }
        }
        log.info("Vytvorene task_submission id={} pre taskId={} studentId={}",
                ts.getId(), ts.getTaskId(), ts.getStudentId());
        return ts;
    }

    /**
     * Upravi existujuce odovzdanie (vrátane hodnotenia).
     */
    public boolean update(TaskSubmission ts) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            if (ts.getSubmittedAt() != null)
                stmt.setObject(1, ts.getSubmittedAt());
            else
                stmt.setNull(1, Types.TIMESTAMP_WITH_TIMEZONE);
            stmt.setString(2, ts.getContent());
            stmt.setString(3, ts.getFileUrl());
            stmt.setString(4, ts.getStatus().name());
            setNullableInt(stmt, 5, ts.getGradedBy());
            if (ts.getGradedAt() != null)
                stmt.setObject(6, ts.getGradedAt());
            else
                stmt.setNull(6, Types.TIMESTAMP_WITH_TIMEZONE);
            stmt.setInt(7, ts.getId());

            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("TaskSubmission id={} uspesne upravene", ts.getId());
            return updated;
        }
    }

    /**
     * Zmeni status odovzdania.
     */
    public boolean updateStatus(int submissionId, TaskSubmission.Status newStatus) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, submissionId);
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("TaskSubmission id={} -> status={}", submissionId, newStatus);
            return updated;
        }
    }

    /**
     * Vymaze odovzdanie podla ID.
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazane task_submission id={}", id);
            return deleted;
        }
    }

    /**
     * Mapovanie ResultSet na objekt TaskSubmission.
     */
    private TaskSubmission mapRow(ResultSet rs) throws SQLException {
        TaskSubmission ts = new TaskSubmission();
        ts.setId(rs.getInt("id"));
        ts.setTaskId(rs.getInt("task_id"));
        ts.setStudentId(rs.getInt("student_id"));
        ts.setSubmittedAt(rs.getObject("submitted_at", java.time.OffsetDateTime.class));
        ts.setContent(rs.getString("content"));
        ts.setFileUrl(rs.getString("file_url"));
        String status = rs.getString("status");
        if (status != null) ts.setStatus(TaskSubmission.Status.valueOf(status));

        int gb = rs.getInt("graded_by");
        if (!rs.wasNull()) ts.setGradedBy(gb);

        ts.setGradedAt(rs.getObject("graded_at", java.time.OffsetDateTime.class));
        return ts;
    }

    // Helper pre nullable INT polia
    private void setNullableInt(PreparedStatement stmt, int idx, Integer val) throws SQLException {
        if (val != null) stmt.setInt(idx, val);
        else stmt.setNull(idx, Types.INTEGER);
    }
}
