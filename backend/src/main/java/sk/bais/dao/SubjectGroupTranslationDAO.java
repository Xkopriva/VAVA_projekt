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

import sk.bais.model.SubjectGroupTranslation;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku subject_group_translation.
 * Kompozitny PK: (subject_group_id, locale) — bez SERIAL id.
 */
public class SubjectGroupTranslationDAO {

    private static final Logger log = LoggerFactory.getLogger(SubjectGroupTranslationDAO.class);

    private static final String SQL_LIST_BY_GROUP =
            "SELECT subject_group_id, locale, name, description " +
            "FROM subject_group_translation WHERE subject_group_id = ?";

    private static final String SQL_GET =
            "SELECT subject_group_id, locale, name, description " +
            "FROM subject_group_translation WHERE subject_group_id = ? AND locale = ?";

    private static final String SQL_INSERT =
            "INSERT INTO subject_group_translation (subject_group_id, locale, name, description) VALUES (?, ?, ?, ?)";

    private static final String SQL_UPSERT =
            "INSERT INTO subject_group_translation (subject_group_id, locale, name, description) VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (subject_group_id, locale) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description";

    private static final String SQL_UPDATE =
            "UPDATE subject_group_translation SET name = ?, description = ? WHERE subject_group_id = ? AND locale = ?";

    private static final String SQL_DELETE =
            "DELETE FROM subject_group_translation WHERE subject_group_id = ? AND locale = ?";

    private static final String SQL_DELETE_ALL =
            "DELETE FROM subject_group_translation WHERE subject_group_id = ?";

    /**
     * Zoznam vsetkych prekladov pre danu skupinu predmetov.
     */
    public List<SubjectGroupTranslation> listByGroup(int subjectGroupId) throws SQLException {
        List<SubjectGroupTranslation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_GROUP)) {
            stmt.setInt(1, subjectGroupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} prekladov pre subjectGroupId={}", list.size(), subjectGroupId);
        return list;
    }

    /**
     * Najde preklad skupiny pre danu lokalitu.
     */
    public Optional<SubjectGroupTranslation> get(int subjectGroupId, String locale) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET)) {
            stmt.setInt(1, subjectGroupId);
            stmt.setString(2, locale);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Prida novy preklad skupiny predmetov.
     */
    public SubjectGroupTranslation create(SubjectGroupTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            stmt.setInt(1, t.getSubjectGroupId());
            stmt.setString(2, t.getLocale());
            stmt.setString(3, t.getName());
            stmt.setString(4, t.getDescription());
            stmt.executeUpdate();
        }
        log.info("Vytvoreny preklad subjectGroupId={} locale={}", t.getSubjectGroupId(), t.getLocale());
        return t;
    }

    /**
     * Vlozi alebo aktualizuje preklad (INSERT ... ON CONFLICT DO UPDATE).
     */
    public SubjectGroupTranslation upsert(SubjectGroupTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPSERT)) {
            stmt.setInt(1, t.getSubjectGroupId());
            stmt.setString(2, t.getLocale());
            stmt.setString(3, t.getName());
            stmt.setString(4, t.getDescription());
            stmt.executeUpdate();
        }
        log.info("Upsert preklad subjectGroupId={} locale={}", t.getSubjectGroupId(), t.getLocale());
        return t;
    }

    /**
     * Upravi existujuci preklad skupiny predmetov.
     */
    public boolean update(SubjectGroupTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, t.getName());
            stmt.setString(2, t.getDescription());
            stmt.setInt(3, t.getSubjectGroupId());
            stmt.setString(4, t.getLocale());
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Preklad subjectGroupId={} locale={} upraveny", t.getSubjectGroupId(), t.getLocale());
            return updated;
        }
    }

    /**
     * Vymaze preklad skupiny predmetov pre danu lokalitu.
     */
    public boolean delete(int subjectGroupId, String locale) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, subjectGroupId);
            stmt.setString(2, locale);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany preklad subjectGroupId={} locale={}", subjectGroupId, locale);
            return deleted;
        }
    }

    /**
     * Vymaze vsetky preklady skupiny predmetov.
     */
    public int deleteAll(int subjectGroupId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL)) {
            stmt.setInt(1, subjectGroupId);
            int count = stmt.executeUpdate();
            log.info("Vymazanych {} prekladov pre subjectGroupId={}", count, subjectGroupId);
            return count;
        }
    }

    private SubjectGroupTranslation mapRow(ResultSet rs) throws SQLException {
        return new SubjectGroupTranslation(
                rs.getInt("subject_group_id"),
                rs.getString("locale"),
                rs.getString("name"),
                rs.getString("description")
        );
    }
}
