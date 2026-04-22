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

import sk.bais.model.EventTranslation;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku event_translation.
 * Kompozitny PK: (event_id, locale) — bez SERIAL id.
 */
public class EventTranslationDAO {

    private static final Logger log = LoggerFactory.getLogger(EventTranslationDAO.class);

    private static final String SQL_LIST_BY_EVENT =
            "SELECT event_id, locale, title, description FROM event_translation WHERE event_id = ?";

    private static final String SQL_GET =
            "SELECT event_id, locale, title, description FROM event_translation WHERE event_id = ? AND locale = ?";

    private static final String SQL_INSERT =
            "INSERT INTO event_translation (event_id, locale, title, description) VALUES (?, ?, ?, ?)";

    private static final String SQL_UPSERT =
            "INSERT INTO event_translation (event_id, locale, title, description) VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (event_id, locale) DO UPDATE SET title = EXCLUDED.title, description = EXCLUDED.description";

    private static final String SQL_UPDATE =
            "UPDATE event_translation SET title = ?, description = ? WHERE event_id = ? AND locale = ?";

    private static final String SQL_DELETE =
            "DELETE FROM event_translation WHERE event_id = ? AND locale = ?";

    private static final String SQL_DELETE_ALL =
            "DELETE FROM event_translation WHERE event_id = ?";

    /**
     * Zoznam vsetkych prekladov pre danu udalost.
     */
    public List<EventTranslation> listByEvent(int eventId) throws SQLException {
        List<EventTranslation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_EVENT)) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} prekladov pre eventId={}", list.size(), eventId);
        return list;
    }

    /**
     * Najde preklad udalosti pre danu lokalitu.
     */
    public Optional<EventTranslation> get(int eventId, String locale) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET)) {
            stmt.setInt(1, eventId);
            stmt.setString(2, locale);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Prida novy preklad udalosti.
     */
    public EventTranslation create(EventTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            stmt.setInt(1, t.getEventId());
            stmt.setString(2, t.getLocale());
            stmt.setString(3, t.getTitle());
            stmt.setString(4, t.getDescription());
            stmt.executeUpdate();
        }
        log.info("Vytvoreny preklad eventId={} locale={}", t.getEventId(), t.getLocale());
        return t;
    }

    /**
     * Vlozi alebo aktualizuje preklad (INSERT ... ON CONFLICT DO UPDATE).
     */
    public EventTranslation upsert(EventTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPSERT)) {
            stmt.setInt(1, t.getEventId());
            stmt.setString(2, t.getLocale());
            stmt.setString(3, t.getTitle());
            stmt.setString(4, t.getDescription());
            stmt.executeUpdate();
        }
        log.info("Upsert preklad eventId={} locale={}", t.getEventId(), t.getLocale());
        return t;
    }

    /**
     * Upravi existujuci preklad udalosti.
     */
    public boolean update(EventTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, t.getTitle());
            stmt.setString(2, t.getDescription());
            stmt.setInt(3, t.getEventId());
            stmt.setString(4, t.getLocale());
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Preklad eventId={} locale={} upraveny", t.getEventId(), t.getLocale());
            return updated;
        }
    }

    /**
     * Vymaze preklad udalosti pre danu lokalitu.
     */
    public boolean delete(int eventId, String locale) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, eventId);
            stmt.setString(2, locale);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany preklad eventId={} locale={}", eventId, locale);
            return deleted;
        }
    }

    /**
     * Vymaze vsetky preklady udalosti.
     */
    public int deleteAll(int eventId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL)) {
            stmt.setInt(1, eventId);
            int count = stmt.executeUpdate();
            log.info("Vymazanych {} prekladov pre eventId={}", count, eventId);
            return count;
        }
    }

    private EventTranslation mapRow(ResultSet rs) throws SQLException {
        return new EventTranslation(
                rs.getInt("event_id"),
                rs.getString("locale"),
                rs.getString("title"),
                rs.getString("description")
        );
    }
}
