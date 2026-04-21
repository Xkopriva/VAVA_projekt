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

import sk.bais.model.SubjectGroup;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku subject_group.
 * Spravuje skupiny predmetov v ramci semestrov.
 */
public class SubjectGroupDAO {

    private static final Logger log = LoggerFactory.getLogger(SubjectGroupDAO.class);

    private static final String COLS = "id, semester_id, sort_order, created_at";

    private static final String SQL_LIST =
            "SELECT " + COLS + " FROM subject_group ORDER BY semester_id, sort_order";

    private static final String SQL_LIST_BY_SEMESTER =
            "SELECT " + COLS + " FROM subject_group WHERE semester_id = ? ORDER BY sort_order";

    private static final String SQL_GET_BY_ID =
            "SELECT " + COLS + " FROM subject_group WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO subject_group (semester_id, sort_order) VALUES (?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE subject_group SET semester_id = ?, sort_order = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM subject_group WHERE id = ?";

    /**
     * Zoznam vsetkych skupin predmetov.
     */
    public List<SubjectGroup> list() throws SQLException {
        List<SubjectGroup> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} subject_group zaznamov", list.size());
        return list;
    }

    /**
     * Zoznam skupin pre dany semester.
     */
    public List<SubjectGroup> listBySemester(int semesterId) throws SQLException {
        List<SubjectGroup> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_SEMESTER)) {
            stmt.setInt(1, semesterId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} skupin pre semesterId={}", list.size(), semesterId);
        return list;
    }

    /**
     * Najde skupinu podla ID.
     */
    public Optional<SubjectGroup> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdena subject_group id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("SubjectGroup id={} nenajdena", id);
        return Optional.empty();
    }

    /**
     * Vytvori novu skupinu predmetov.
     */
    public SubjectGroup create(SubjectGroup g) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, g.getSemesterId());
            stmt.setInt(2, g.getSortOrder());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) g.setId(keys.getInt(1));
            }
        }
        log.info("Vytvorena subject_group id={} pre semesterId={}", g.getId(), g.getSemesterId());
        return g;
    }

    /**
     * Upravi existujucu skupinu predmetov.
     */
    public boolean update(SubjectGroup g) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setInt(1, g.getSemesterId());
            stmt.setInt(2, g.getSortOrder());
            stmt.setInt(3, g.getId());

            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("SubjectGroup id={} uspesne upravena", g.getId());
            return updated;
        }
    }

    /**
     * Vymaze skupinu predmetov podla ID.
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazana subject_group id={}", id);
            return deleted;
        }
    }

    /**
     * Mapovanie ResultSet na objekt SubjectGroup.
     */
    private SubjectGroup mapRow(ResultSet rs) throws SQLException {
        SubjectGroup g = new SubjectGroup();
        g.setId(rs.getInt("id"));
        g.setSemesterId(rs.getInt("semester_id"));
        g.setSortOrder(rs.getInt("sort_order"));
        g.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return g;
    }
}
