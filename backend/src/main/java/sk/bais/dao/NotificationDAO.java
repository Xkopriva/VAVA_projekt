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

import sk.bais.model.Notification;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku notification.
 * Spravuje notifikacie pouzivatelov.
 * Obsah je generovany v locale prijaleca v case odoslania — nie je opatovne prekladany.
 */
public class NotificationDAO {

    private static final Logger log = LoggerFactory.getLogger(NotificationDAO.class);

    private static final String COLS =
            "id, recipient_id, sender_id, type, title, message, is_read, " +
            "related_mark_id, related_subject_id, related_task_id, created_at, read_at";

    private static final String SQL_LIST_BY_RECIPIENT =
            "SELECT " + COLS + " FROM notification " +
            "WHERE recipient_id = ? ORDER BY created_at DESC";

    private static final String SQL_LIST_UNREAD_BY_RECIPIENT =
            "SELECT " + COLS + " FROM notification " +
            "WHERE recipient_id = ? AND is_read = FALSE ORDER BY created_at DESC";

    private static final String SQL_GET_BY_ID =
            "SELECT " + COLS + " FROM notification WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO notification " +
            "(recipient_id, sender_id, type, title, message, " +
            " related_mark_id, related_subject_id, related_task_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_MARK_AS_READ =
            "UPDATE notification SET is_read = TRUE, read_at = NOW() WHERE id = ? AND recipient_id = ?";

    private static final String SQL_MARK_ALL_READ =
            "UPDATE notification SET is_read = TRUE, read_at = NOW() " +
            "WHERE recipient_id = ? AND is_read = FALSE";

    private static final String SQL_DELETE =
            "DELETE FROM notification WHERE id = ?";

    /**
     * Zoznam vsetkych notifikacii pouzivatea (najnovsie prve).
     */
    public List<Notification> listByRecipient(int recipientId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_RECIPIENT)) {
            stmt.setInt(1, recipientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} notifikacii pre recipientId={}", list.size(), recipientId);
        return list;
    }

    /**
     * Zoznam neprecenych notifikacii pouzivatea.
     */
    public List<Notification> listUnreadByRecipient(int recipientId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_UNREAD_BY_RECIPIENT)) {
            stmt.setInt(1, recipientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} neprecitanych notifikacii pre recipientId={}", list.size(), recipientId);
        return list;
    }

    /**
     * Najde notifikaciu podla ID.
     */
    public Optional<Notification> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdena notification id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("Notification id={} nenajdena", id);
        return Optional.empty();
    }

    /**
     * Vytvori novu notifikaciu.
     */
    public Notification create(Notification n) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, n.getRecipientId());
            setNullableInt(stmt, 2, n.getSenderId());
            stmt.setString(3, n.getType().name());
            stmt.setString(4, n.getTitle());
            stmt.setString(5, n.getMessage());
            setNullableInt(stmt, 6, n.getRelatedMarkId());
            setNullableInt(stmt, 7, n.getRelatedSubjectId());
            setNullableInt(stmt, 8, n.getRelatedTaskId());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) n.setId(keys.getInt(1));
            }
        }
        log.info("Vytvorena notification id={} pre recipientId={} type={}",
                n.getId(), n.getRecipientId(), n.getType());
        return n;
    }

    /**
     * Oznaci notifikaciu ako precitanu.
     *
     * @param id          ID notifikacie
     * @param recipientId ID prijaleca (ochrana pred cudzim pristupom)
     * @return true ak bola notifikacia aktualizovana
     */
    public boolean markAsRead(int id, int recipientId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_MARK_AS_READ)) {
            stmt.setInt(1, id);
            stmt.setInt(2, recipientId);
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Notification id={} oznacena ako precitana", id);
            return updated;
        }
    }

    /**
     * Oznaci vsetky notifikacie pouzivatea ako precitane.
     *
     * @return pocet aktualizovanych zaznamov
     */
    public int markAllRead(int recipientId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_MARK_ALL_READ)) {
            stmt.setInt(1, recipientId);
            int count = stmt.executeUpdate();
            log.info("Oznacenych {} notifikacii ako precitane pre recipientId={}", count, recipientId);
            return count;
        }
    }

    /**
     * Vymaze notifikaciu podla ID.
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazana notification id={}", id);
            return deleted;
        }
    }

    /**
     * Mapovanie ResultSet na objekt Notification.
     */
    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setRecipientId(rs.getInt("recipient_id"));

        int sid = rs.getInt("sender_id");
        if (!rs.wasNull()) n.setSenderId(sid);

        String type = rs.getString("type");
        if (type != null) n.setType(Notification.Type.valueOf(type));

        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getBoolean("is_read"));

        int mid = rs.getInt("related_mark_id");
        if (!rs.wasNull()) n.setRelatedMarkId(mid);

        int subjId = rs.getInt("related_subject_id");
        if (!rs.wasNull()) n.setRelatedSubjectId(subjId);

        int tid = rs.getInt("related_task_id");
        if (!rs.wasNull()) n.setRelatedTaskId(tid);

        n.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        n.setReadAt(rs.getObject("read_at", java.time.OffsetDateTime.class));
        return n;
    }

    // Helper pre nullable INT polia
    private void setNullableInt(PreparedStatement stmt, int idx, Integer val) throws SQLException {
        if (val != null) stmt.setInt(idx, val);
        else stmt.setNull(idx, Types.INTEGER);
    }
}
