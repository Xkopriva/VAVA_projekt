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

import sk.bais.model.SubjectTranslation;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku subject_translation.
 * Kompozitny PK: (subject_id, locale) — bez SERIAL id.
 */
public class SubjectTranslationDAO {

    private static final Logger log = LoggerFactory.getLogger(SubjectTranslationDAO.class);

    private static final String SQL_LIST_BY_SUBJECT =
            "SELECT subject_id, locale, name, description FROM subject_translation WHERE subject_id = ?";

    private static final String SQL_GET =
            "SELECT subject_id, locale, name, description FROM subject_translation WHERE subject_id = ? AND locale = ?";

    private static final String SQL_INSERT =
            "INSERT INTO subject_translation (subject_id, locale, name, description) VALUES (?, ?, ?, ?)";

    private static final String SQL_UPSERT =
            "INSERT INTO subject_translation (subject_id, locale, name, description) VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (subject_id, locale) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description";

    private static final String SQL_UPDATE =
            "UPDATE subject_translation SET name = ?, description = ? WHERE subject_id = ? AND locale = ?";

    private static final String SQL_DELETE =
            "DELETE FROM subject_translation WHERE subject_id = ? AND locale = ?";

    private static final String SQL_DELETE_ALL =
            "DELETE FROM subject_translation WHERE subject_id = ?";

    /**
     * Zoznam vsetkych prekladov pre dany predmet.
     */
    public List<SubjectTranslation> listBySubject(int subjectId) throws SQLException {
        List<SubjectTranslation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_SUBJECT)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} prekladov pre subjectId={}", list.size(), subjectId);
        return list;
    }

    /**
     * Najde preklad predmetu pre danu lokalitu.
     */
    public Optional<SubjectTranslation> get(int subjectId, String locale) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET)) {
            stmt.setInt(1, subjectId);
            stmt.setString(2, locale);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Prida novy preklad predmetu.
     */
    public SubjectTranslation create(SubjectTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            stmt.setInt(1, t.getSubjectId());
            stmt.setString(2, t.getLocale());
            stmt.setString(3, t.getName());
            stmt.setString(4, t.getDescription());
            stmt.executeUpdate();
        }
        log.info("Vytvoreny preklad subjectId={} locale={}", t.getSubjectId(), t.getLocale());
        return t;
    }

    /**
     * Vlozi alebo aktualizuje preklad (INSERT ... ON CONFLICT DO UPDATE).
     */
    public SubjectTranslation upsert(SubjectTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPSERT)) {
            stmt.setInt(1, t.getSubjectId());
            stmt.setString(2, t.getLocale());
            stmt.setString(3, t.getName());
            stmt.setString(4, t.getDescription());
            stmt.executeUpdate();
        }
        log.info("Upsert preklad subjectId={} locale={}", t.getSubjectId(), t.getLocale());
        return t;
    }

    /**
     * Upravi existujuci preklad predmetu.
     */
    public boolean update(SubjectTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, t.getName());
            stmt.setString(2, t.getDescription());
            stmt.setInt(3, t.getSubjectId());
            stmt.setString(4, t.getLocale());
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Preklad subjectId={} locale={} upraveny", t.getSubjectId(), t.getLocale());
            return updated;
        }
    }

    /**
     * Vymaze preklad predmetu pre danu lokalitu.
     */
    public boolean delete(int subjectId, String locale) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, subjectId);
            stmt.setString(2, locale);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany preklad subjectId={} locale={}", subjectId, locale);
            return deleted;
        }
    }

    /**
     * Vymaze vsetky preklady predmetu.
     */
    public int deleteAll(int subjectId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL)) {
            stmt.setInt(1, subjectId);
            int count = stmt.executeUpdate();
            log.info("Vymazanych {} prekladov pre subjectId={}", count, subjectId);
            return count;
        }
    }

    private SubjectTranslation mapRow(ResultSet rs) throws SQLException {
        return new SubjectTranslation(
                rs.getInt("subject_id"),
                rs.getString("locale"),
                rs.getString("name"),
                rs.getString("description")
        );
    }
}
