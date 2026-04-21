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

import sk.bais.model.SemesterTranslation;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku semester_translation.
 * Kompozitny PK: (semester_id, locale) — bez SERIAL id.
 */
public class SemesterTranslationDAO {

    private static final Logger log = LoggerFactory.getLogger(SemesterTranslationDAO.class);

    private static final String SQL_LIST_BY_SEMESTER =
            "SELECT semester_id, locale, name FROM semester_translation WHERE semester_id = ?";

    private static final String SQL_GET =
            "SELECT semester_id, locale, name FROM semester_translation WHERE semester_id = ? AND locale = ?";

    private static final String SQL_INSERT =
            "INSERT INTO semester_translation (semester_id, locale, name) VALUES (?, ?, ?)";

    private static final String SQL_UPSERT =
            "INSERT INTO semester_translation (semester_id, locale, name) VALUES (?, ?, ?) " +
            "ON CONFLICT (semester_id, locale) DO UPDATE SET name = EXCLUDED.name";

    private static final String SQL_UPDATE =
            "UPDATE semester_translation SET name = ? WHERE semester_id = ? AND locale = ?";

    private static final String SQL_DELETE =
            "DELETE FROM semester_translation WHERE semester_id = ? AND locale = ?";

    private static final String SQL_DELETE_ALL =
            "DELETE FROM semester_translation WHERE semester_id = ?";

    /**
     * Zoznam vsetkych prekladov pre dany semester.
     */
    public List<SemesterTranslation> listBySemester(int semesterId) throws SQLException {
        List<SemesterTranslation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_SEMESTER)) {
            stmt.setInt(1, semesterId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} prekladov pre semesterId={}", list.size(), semesterId);
        return list;
    }

    /**
     * Najde preklad semestra pre danu lokalitu.
     */
    public Optional<SemesterTranslation> get(int semesterId, String locale) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET)) {
            stmt.setInt(1, semesterId);
            stmt.setString(2, locale);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Prida novy preklad semestra.
     */
    public SemesterTranslation create(SemesterTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            stmt.setInt(1, t.getSemesterId());
            stmt.setString(2, t.getLocale());
            stmt.setString(3, t.getName());
            stmt.executeUpdate();
        }
        log.info("Vytvoreny preklad semesterId={} locale={}", t.getSemesterId(), t.getLocale());
        return t;
    }

    /**
     * Vlozi alebo aktualizuje preklad (INSERT ... ON CONFLICT DO UPDATE).
     */
    public SemesterTranslation upsert(SemesterTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPSERT)) {
            stmt.setInt(1, t.getSemesterId());
            stmt.setString(2, t.getLocale());
            stmt.setString(3, t.getName());
            stmt.executeUpdate();
        }
        log.info("Upsert preklad semesterId={} locale={}", t.getSemesterId(), t.getLocale());
        return t;
    }

    /**
     * Upravi existujuci preklad semestra.
     */
    public boolean update(SemesterTranslation t) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, t.getName());
            stmt.setInt(2, t.getSemesterId());
            stmt.setString(3, t.getLocale());
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Preklad semesterId={} locale={} upraveny", t.getSemesterId(), t.getLocale());
            return updated;
        }
    }

    /**
     * Vymaze preklad semestra pre danu lokalitu.
     */
    public boolean delete(int semesterId, String locale) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, semesterId);
            stmt.setString(2, locale);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany preklad semesterId={} locale={}", semesterId, locale);
            return deleted;
        }
    }

    /**
     * Vymaze vsetky preklady semestra (napr. pred vymazanim semestra).
     */
    public int deleteAll(int semesterId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL)) {
            stmt.setInt(1, semesterId);
            int count = stmt.executeUpdate();
            log.info("Vymazanych {} prekladov pre semesterId={}", count, semesterId);
            return count;
        }
    }

    private SemesterTranslation mapRow(ResultSet rs) throws SQLException {
        return new SemesterTranslation(
                rs.getInt("semester_id"),
                rs.getString("locale"),
                rs.getString("name")
        );
    }
}
