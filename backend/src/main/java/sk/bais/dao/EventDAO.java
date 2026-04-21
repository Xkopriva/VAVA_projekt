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

import sk.bais.model.Event;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku event.
 * Spravuje naplanovane udalosti predmetov (prednasky, cvicenia, skusky, ...).
 */
public class EventDAO {

    private static final Logger log = LoggerFactory.getLogger(EventDAO.class);

    private static final String COLS =
            "id, subject_id, type, week_number, room, scheduled_at, " +
            "duration_minutes, is_published, created_by, created_at, updated_at";

    private static final String SQL_LIST =
            "SELECT " + COLS + " FROM event ORDER BY scheduled_at NULLS LAST, id";

    private static final String SQL_LIST_BY_SUBJECT =
            "SELECT " + COLS + " FROM event WHERE subject_id = ? ORDER BY scheduled_at NULLS LAST";

    private static final String SQL_GET_BY_ID =
            "SELECT " + COLS + " FROM event WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO event (subject_id, type, week_number, room, scheduled_at, " +
            "duration_minutes, is_published, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE event SET subject_id = ?, type = ?, week_number = ?, room = ?, " +
            "scheduled_at = ?, duration_minutes = ?, is_published = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM event WHERE id = ?";

    /**
     * Zoznam vsetkych udalosti.
     */
    public List<Event> list() throws SQLException {
        List<Event> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} event zaznamov", list.size());
        return list;
    }

    /**
     * Zoznam udalosti pre dany predmet.
     */
    public List<Event> listBySubject(int subjectId) throws SQLException {
        List<Event> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_SUBJECT)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} udalosti pre subjectId={}", list.size(), subjectId);
        return list;
    }

    /**
     * Najde udalost podla ID.
     */
    public Optional<Event> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdeny event id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("Event id={} nenajdeny", id);
        return Optional.empty();
    }

    /**
     * Vytvori novu udalost.
     */
    public Event create(Event e) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, e.getSubjectId());
            stmt.setString(2, e.getType().name());
            setNullableInt(stmt, 3, e.getWeekNumber());
            stmt.setString(4, e.getRoom());
            if (e.getScheduledAt() != null)
                stmt.setObject(5, e.getScheduledAt());
            else
                stmt.setNull(5, Types.TIMESTAMP_WITH_TIMEZONE);
            setNullableInt(stmt, 6, e.getDurationMinutes());
            stmt.setBoolean(7, e.isPublished());
            setNullableInt(stmt, 8, e.getCreatedBy());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) e.setId(keys.getInt(1));
            }
        }
        log.info("Vytvoreny event id={} pre subjectId={}", e.getId(), e.getSubjectId());
        return e;
    }

    /**
     * Upravi existujucu udalost.
     */
    public boolean update(Event e) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setInt(1, e.getSubjectId());
            stmt.setString(2, e.getType().name());
            setNullableInt(stmt, 3, e.getWeekNumber());
            stmt.setString(4, e.getRoom());
            if (e.getScheduledAt() != null)
                stmt.setObject(5, e.getScheduledAt());
            else
                stmt.setNull(5, Types.TIMESTAMP_WITH_TIMEZONE);
            setNullableInt(stmt, 6, e.getDurationMinutes());
            stmt.setBoolean(7, e.isPublished());
            stmt.setInt(8, e.getId());

            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Event id={} uspesne upraveny", e.getId());
            return updated;
        }
    }

    /**
     * Vymaze udalost podla ID.
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany event id={}", id);
            return deleted;
        }
    }

    /**
     * Mapovanie ResultSet na objekt Event.
     */
    private Event mapRow(ResultSet rs) throws SQLException {
        Event e = new Event();
        e.setId(rs.getInt("id"));
        e.setSubjectId(rs.getInt("subject_id"));
        String type = rs.getString("type");
        if (type != null) e.setType(Event.Type.valueOf(type));

        int wn = rs.getInt("week_number");
        if (!rs.wasNull()) e.setWeekNumber(wn);

        e.setRoom(rs.getString("room"));
        e.setScheduledAt(rs.getObject("scheduled_at", java.time.OffsetDateTime.class));

        int dur = rs.getInt("duration_minutes");
        if (!rs.wasNull()) e.setDurationMinutes(dur);

        e.setPublished(rs.getBoolean("is_published"));

        int cb = rs.getInt("created_by");
        if (!rs.wasNull()) e.setCreatedBy(cb);

        e.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        e.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return e;
    }

    // Helper pre nullable INT polia
    private void setNullableInt(PreparedStatement stmt, int idx, Integer val) throws SQLException {
        if (val != null) stmt.setInt(idx, val);
        else stmt.setNull(idx, Types.INTEGER);
    }
}
