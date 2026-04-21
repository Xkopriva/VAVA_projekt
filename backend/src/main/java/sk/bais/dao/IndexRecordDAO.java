package sk.bais.dao;

import java.sql.Connection;
import java.sql.Date;
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

import sk.bais.model.IndexRecord;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku index_record.
 * Kazdy enrollment ma maximalne jeden IndexRecord (UNIQUE constraint).
 */
public class IndexRecordDAO {

    private static final Logger log = LoggerFactory.getLogger(IndexRecordDAO.class);

    private static final String SQL_LIST =
            "SELECT id, enrollment_id, recorded_by, final_mark, " +
            "       recorded_at, exam_date, notes " +
            "FROM index_record ORDER BY recorded_at DESC";

    private static final String SQL_GET_BY_ID =
            "SELECT id, enrollment_id, recorded_by, final_mark, " +
            "       recorded_at, exam_date, notes " +
            "FROM index_record WHERE id = ?";

    private static final String SQL_GET_BY_ENROLLMENT =
            "SELECT id, enrollment_id, recorded_by, final_mark, " +
            "       recorded_at, exam_date, notes " +
            "FROM index_record WHERE enrollment_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO index_record (enrollment_id, recorded_by, final_mark, exam_date, notes) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE index_record SET recorded_by = ?, final_mark = ?, " +
            "exam_date = ?, notes = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM index_record WHERE id = ?";

    // LIST
    public List<IndexRecord> list() throws SQLException {
        List<IndexRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {

                list.add(mapRow(rs));
            }
        }
        log.info("Nacitanych {} index record zaznamov", list.size());
        return list;
    }

    // GET BY ID
    public Optional<IndexRecord> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdeny index record id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("Index record id={} nenajdeny", id);
        return Optional.empty();
    }

    // GET BY ENROLLMENT
    // Najde znamku pre konkretny enrollment
    public Optional<IndexRecord> getByEnrollment(int enrollmentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ENROLLMENT)) { 
            
            stmt.setInt(1, enrollmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    IndexRecord record = mapRow(rs);
                    log.debug("Najdeny index record id={} s enrollmentId={}", record.getId(), enrollmentId);

                    return Optional.of(record);
                }
            }
        }
        log.debug("Index record s enrollmentId={} nenajdeny", enrollmentId);
        return Optional.empty();
    }
    // CREATE
    public IndexRecord create(IndexRecord r) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, r.getEnrollmentId());
            if (r.getRecordedBy() != null) stmt.setInt(2, r.getRecordedBy());
            else stmt.setNull(2, Types.INTEGER);
            stmt.setString(3, r.getFinalMark());
            if (r.getExamDate() != null) stmt.setDate(4, Date.valueOf(r.getExamDate()));
            else stmt.setNull(4, Types.DATE);
            stmt.setString(5, r.getNotes());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) r.setId(keys.getInt(1));
            }
        }
        log.info("Vytvoreny index record id={}", r.getId());
        return r;
    }

    // UPDATE
    public boolean update(IndexRecord r) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            if (r.getRecordedBy() != null) stmt.setInt(1, r.getRecordedBy());
            else stmt.setNull(1, Types.INTEGER);
            stmt.setString(2, r.getFinalMark());
            if (r.getExamDate() != null) stmt.setDate(3, Date.valueOf(r.getExamDate()));
            else stmt.setNull(3, Types.DATE);
            stmt.setString(4, r.getNotes());
            stmt.setInt(5, r.getId());

            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Index record id={} uspesne upraveny", r.getId());
            return updated;
        }
    }

    // DELETE
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany index record id={}", id);
            return deleted;
        }
    }

    //MAPPER 
    private IndexRecord mapRow(ResultSet rs) throws SQLException {
        IndexRecord r = new IndexRecord();
        r.setId(rs.getInt("id"));
        r.setEnrollmentId(rs.getInt("enrollment_id"));
        int rb = rs.getInt("recorded_by");
        if (!rs.wasNull()) r.setRecordedBy(rb);
        r.setFinalMark(rs.getString("final_mark"));
        r.setRecordedAt(rs.getObject("recorded_at", java.time.OffsetDateTime.class));
        java.sql.Date ed = rs.getDate("exam_date");
        if (ed != null) r.setExamDate(ed.toLocalDate());
        r.setNotes(rs.getString("notes"));
        return r;
    }
}