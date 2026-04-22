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

import sk.bais.model.Task;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku task.
 * Spravuje zadania vytvorene ucitelmi — title/description nie su lokalizovane.
 */
public class TaskDAO {

    private static final Logger log = LoggerFactory.getLogger(TaskDAO.class);

    private static final String COLS =
            "id, event_id, subject_id, title, description, due_at, " +
            "max_points, is_published, created_by, created_at, updated_at";

    private static final String SQL_LIST =
            "SELECT " + COLS + " FROM task ORDER BY due_at NULLS LAST, id";

    private static final String SQL_LIST_BY_SUBJECT =
            "SELECT " + COLS + " FROM task WHERE subject_id = ? ORDER BY due_at NULLS LAST";

    private static final String SQL_GET_BY_ID =
            "SELECT " + COLS + " FROM task WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO task (event_id, subject_id, title, description, due_at, " +
            "max_points, is_published, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE task SET event_id = ?, subject_id = ?, title = ?, description = ?, " +
            "due_at = ?, max_points = ?, is_published = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM task WHERE id = ?";

    /**
     * Zoznam vsetkych zadani.
     */
    public List<Task> list() throws SQLException {
        List<Task> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} task zaznamov", list.size());
        return list;
    }

    /**
     * Zoznam zadani pre dany predmet.
     */
    public List<Task> listBySubject(int subjectId) throws SQLException {
        List<Task> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_SUBJECT)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} zadani pre subjectId={}", list.size(), subjectId);
        return list;
    }

    /**
     * Najde zadanie podla ID.
     */
    public Optional<Task> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdene task id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("Task id={} nenajdene", id);
        return Optional.empty();
    }

    /**
     * Vytvori nove zadanie.
     */
    public Task create(Task t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            setNullableInt(stmt, 1, t.getEventId());
            stmt.setInt(2, t.getSubjectId());
            stmt.setString(3, t.getTitle());
            stmt.setString(4, t.getDescription());
            if (t.getDueAt() != null)
                stmt.setObject(5, t.getDueAt());
            else
                stmt.setNull(5, Types.TIMESTAMP_WITH_TIMEZONE);
            if (t.getMaxPoints() != null)
                stmt.setBigDecimal(6, t.getMaxPoints());
            else
                stmt.setNull(6, Types.NUMERIC);
            stmt.setBoolean(7, t.isPublished());
            setNullableInt(stmt, 8, t.getCreatedBy());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) t.setId(keys.getInt(1));
            }
        }
        log.info("Vytvorene task id={} pre subjectId={}", t.getId(), t.getSubjectId());
        return t;
    }

    /**
     * Upravi existujuce zadanie.
     */
    public boolean update(Task t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            setNullableInt(stmt, 1, t.getEventId());
            stmt.setInt(2, t.getSubjectId());
            stmt.setString(3, t.getTitle());
            stmt.setString(4, t.getDescription());
            if (t.getDueAt() != null)
                stmt.setObject(5, t.getDueAt());
            else
                stmt.setNull(5, Types.TIMESTAMP_WITH_TIMEZONE);
            if (t.getMaxPoints() != null)
                stmt.setBigDecimal(6, t.getMaxPoints());
            else
                stmt.setNull(6, Types.NUMERIC);
            stmt.setBoolean(7, t.isPublished());
            stmt.setInt(8, t.getId());

            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Task id={} uspesne upravene", t.getId());
            return updated;
        }
    }

    /**
     * Vymaze zadanie podla ID.
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazane task id={}", id);
            return deleted;
        }
    }

    /**
     * Mapovanie ResultSet na objekt Task.
     */
    private Task mapRow(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getInt("id"));

        int eventId = rs.getInt("event_id");
        if (!rs.wasNull()) t.setEventId(eventId);

        t.setSubjectId(rs.getInt("subject_id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setDueAt(rs.getObject("due_at", java.time.OffsetDateTime.class));
        t.setMaxPoints(rs.getBigDecimal("max_points"));
        t.setPublished(rs.getBoolean("is_published"));

        int cb = rs.getInt("created_by");
        if (!rs.wasNull()) t.setCreatedBy(cb);

        t.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        t.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return t;
    }

    // Helper pre nullable INT polia
    private void setNullableInt(PreparedStatement stmt, int idx, Integer val) throws SQLException {
        if (val != null) stmt.setInt(idx, val);
        else stmt.setNull(idx, Types.INTEGER);
    }
}
