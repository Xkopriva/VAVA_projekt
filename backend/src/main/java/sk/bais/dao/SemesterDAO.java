package sk.bais.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.bais.model.Semester;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku semester.
 */
public class SemesterDAO {

    private static final Logger log = LoggerFactory.getLogger(SemesterDAO.class);

    private static final String COLS = "id, code, type, academic_year, start_date, end_date, status, created_at";

    private static final String SQL_LIST =
            "SELECT " + COLS + " FROM semester ORDER BY id";

    private static final String SQL_GET_BY_ID =
            "SELECT " + COLS + " FROM semester WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO semester (code, type, academic_year, start_date, end_date, status) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE semester SET code = ?, type = ?, academic_year = ?, " +
            "start_date = ?, end_date = ?, status = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM semester WHERE id = ?";

    /**
     * Zoznam vsetkych semestrov.
     */
    public List<Semester> list() throws SQLException {
        List<Semester> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} semester zaznamov", list.size());
        return list;
    }

    /**
     * Najde semester podla ID.
     */
    public Optional<Semester> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Vytvori novy semester.
     */
    public Semester create(Semester s) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, s.getCode());
            stmt.setString(2, s.getType().name());
            stmt.setString(3, s.getAcademicYear());
            stmt.setDate(4, s.getStartDate() != null ? Date.valueOf(s.getStartDate()) : null);
            stmt.setDate(5, s.getEndDate() != null ? Date.valueOf(s.getEndDate()) : null);
            stmt.setString(6, s.getStatus().name());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
        }
        log.info("Vytvoreny semester id={}", s.getId());
        return s;
    }

    /**
     * Upravi existujuci semester.
     */
    public boolean update(Semester s) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            
            stmt.setString(1, s.getCode());
            stmt.setString(2, s.getType().name());
            stmt.setString(3, s.getAcademicYear());
            stmt.setDate(4, s.getStartDate() != null ? Date.valueOf(s.getStartDate()) : null);
            stmt.setDate(5, s.getEndDate() != null ? Date.valueOf(s.getEndDate()) : null);
            stmt.setString(6, s.getStatus().name());
            stmt.setInt(7, s.getId());

            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Semester id={} uspesne upraveny", s.getId());
            return updated;
        }
    }

    /**
     * Vymaze semester podla ID.
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany semester id={}", id);
            return deleted;
        }
    }

    /**
     * Mapovanie ResultSet na objekt Semester.
     */
    private Semester mapRow(ResultSet rs) throws SQLException {
        Semester s = new Semester();
        s.setId(rs.getInt("id"));
        s.setCode(rs.getString("code"));
        s.setType(Semester.Type.valueOf(rs.getString("type")));
        s.setAcademicYear(rs.getString("academic_year"));
        
        Date sd = rs.getDate("start_date");
        if (sd != null) s.setStartDate(sd.toLocalDate());
        
        Date ed = rs.getDate("end_date");
        if (ed != null) s.setEndDate(ed.toLocalDate());
        
        s.setStatus(Semester.Status.valueOf(rs.getString("status")));
        s.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return s;
    }
}
