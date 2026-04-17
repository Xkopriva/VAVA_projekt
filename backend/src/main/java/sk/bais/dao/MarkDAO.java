package sk.bais.dao;

import sk.bais.model.Mark;
import sk.bais.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO trieda pre tabulku mark.
 */
public class MarkDAO {

    private static final String SQL_LIST =
            "SELECT id, enrollment_id, event_id, task_submission_id, " +
            "       title, points, max_points, given_by, given_at, notes " +
            "FROM mark ORDER BY given_at DESC";

    private static final String SQL_LIST_BY_ENROLLMENT =
            "SELECT id, enrollment_id, event_id, task_submission_id, " +
            "       title, points, max_points, given_by, given_at, notes " +
            "FROM mark WHERE enrollment_id = ? ORDER BY given_at DESC";

    private static final String SQL_GET_BY_ID =
            "SELECT id, enrollment_id, event_id, task_submission_id, " +
            "       title, points, max_points, given_by, given_at, notes " +
            "FROM mark WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO mark (enrollment_id, event_id, task_submission_id, " +
            "                  title, points, max_points, given_by, notes) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE mark SET title = ?, points = ?, max_points = ?, notes = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM mark WHERE id = ?";

    public List<Mark> list() throws SQLException {
        List<Mark> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // Vrati vsetky znamky pre dany enrollment — najcastejsi use-case
    public List<Mark> listByEnrollment(int enrollmentId) throws SQLException {
        List<Mark> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_ENROLLMENT)) {
            stmt.setInt(1, enrollmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Optional<Mark> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Mark create(Mark m) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, m.getEnrollmentId());
            setNullableInt(stmt, 2, m.getEventId());
            setNullableInt(stmt, 3, m.getTaskSubmissionId());
            stmt.setString(4, m.getTitle());
            stmt.setBigDecimal(5, m.getPoints());
            stmt.setBigDecimal(6, m.getMaxPoints());
            setNullableInt(stmt, 7, m.getGivenBy());
            stmt.setString(8, m.getNotes());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) m.setId(keys.getInt(1));
            }
        }
        return m;
    }

    public boolean update(Mark m) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, m.getTitle());
            stmt.setBigDecimal(2, m.getPoints());
            stmt.setBigDecimal(3, m.getMaxPoints());
            stmt.setString(4, m.getNotes());
            stmt.setInt(5, m.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private Mark mapRow(ResultSet rs) throws SQLException {
        Mark m = new Mark();
        m.setId(rs.getInt("id"));
        m.setEnrollmentId(rs.getInt("enrollment_id"));
        int eid = rs.getInt("event_id");
        if (!rs.wasNull()) m.setEventId(eid);
        int tsid = rs.getInt("task_submission_id");
        if (!rs.wasNull()) m.setTaskSubmissionId(tsid);
        m.setTitle(rs.getString("title"));
        m.setPoints(rs.getBigDecimal("points"));
        m.setMaxPoints(rs.getBigDecimal("max_points"));
        int gb = rs.getInt("given_by");
        if (!rs.wasNull()) m.setGivenBy(gb);
        m.setGivenAt(rs.getObject("given_at", java.time.OffsetDateTime.class));
        m.setNotes(rs.getString("notes"));
        return m;
    }

    private void setNullableInt(PreparedStatement stmt, int idx, Integer val) throws SQLException {
        if (val != null) stmt.setInt(idx, val);
        else stmt.setNull(idx, Types.INTEGER);
    }
}