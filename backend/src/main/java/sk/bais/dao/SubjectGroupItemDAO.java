package sk.bais.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.bais.model.SubjectGroupItem;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku subject_group_item.
 * Kompozitny PK: (subject_group_id, subject_id) — spravuje predmety v skupinach.
 */
public class SubjectGroupItemDAO {

    private static final Logger log = LoggerFactory.getLogger(SubjectGroupItemDAO.class);

    private static final String SQL_LIST_BY_GROUP =
            "SELECT subject_group_id, subject_id, is_mandatory " +
            "FROM subject_group_item WHERE subject_group_id = ? ORDER BY subject_id";

    private static final String SQL_LIST_BY_SUBJECT =
            "SELECT subject_group_id, subject_id, is_mandatory " +
            "FROM subject_group_item WHERE subject_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO subject_group_item (subject_group_id, subject_id, is_mandatory) VALUES (?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE subject_group_item SET is_mandatory = ? WHERE subject_group_id = ? AND subject_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM subject_group_item WHERE subject_group_id = ? AND subject_id = ?";

    private static final String SQL_DELETE_ALL_BY_GROUP =
            "DELETE FROM subject_group_item WHERE subject_group_id = ?";

    private static final String SQL_EXISTS =
            "SELECT 1 FROM subject_group_item WHERE subject_group_id = ? AND subject_id = ?";

    /**
     * Zoznam vsetkych predmetov v danej skupine.
     */
    public List<SubjectGroupItem> listByGroup(int subjectGroupId) throws SQLException {
        List<SubjectGroupItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_GROUP)) {
            stmt.setInt(1, subjectGroupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} predmetov pre subjectGroupId={}", list.size(), subjectGroupId);
        return list;
    }

    /**
     * Zoznam vsetkych skupin v ktorych sa nachadza dany predmet.
     */
    public List<SubjectGroupItem> listBySubject(int subjectId) throws SQLException {
        List<SubjectGroupItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_SUBJECT)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        log.debug("Nacitanych {} skupin pre subjectId={}", list.size(), subjectId);
        return list;
    }

    /**
     * Prida predmet do skupiny.
     */
    public SubjectGroupItem add(SubjectGroupItem item) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            stmt.setInt(1, item.getSubjectGroupId());
            stmt.setInt(2, item.getSubjectId());
            stmt.setBoolean(3, item.isMandatory());
            stmt.executeUpdate();
        }
        log.info("Pridany predmet subjectId={} do skupiny subjectGroupId={} (mandatory={})",
                item.getSubjectId(), item.getSubjectGroupId(), item.isMandatory());
        return item;
    }

    /**
     * Zmeni prednask ci je predmet povinny v danej skupine.
     */
    public boolean updateMandatory(int subjectGroupId, int subjectId, boolean isMandatory) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setBoolean(1, isMandatory);
            stmt.setInt(2, subjectGroupId);
            stmt.setInt(3, subjectId);
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("SubjectGroupItem subjectGroupId={} subjectId={} -> mandatory={}",
                    subjectGroupId, subjectId, isMandatory);
            return updated;
        }
    }

    /**
     * Odstra predmet zo skupiny.
     */
    public boolean remove(int subjectGroupId, int subjectId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, subjectGroupId);
            stmt.setInt(2, subjectId);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Odstraneny predmet subjectId={} zo skupiny subjectGroupId={}",
                    subjectId, subjectGroupId);
            return deleted;
        }
    }

    /**
     * Odstra vsetky predmety z danej skupiny.
     */
    public int removeAll(int subjectGroupId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL_BY_GROUP)) {
            stmt.setInt(1, subjectGroupId);
            int count = stmt.executeUpdate();
            log.info("Odstrananych {} predmetov zo skupiny subjectGroupId={}", count, subjectGroupId);
            return count;
        }
    }

    /**
     * Skontroluje ci predmet uz patri do danej skupiny.
     */
    public boolean exists(int subjectGroupId, int subjectId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_EXISTS)) {
            stmt.setInt(1, subjectGroupId);
            stmt.setInt(2, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private SubjectGroupItem mapRow(ResultSet rs) throws SQLException {
        return new SubjectGroupItem(
                rs.getInt("subject_group_id"),
                rs.getInt("subject_id"),
                rs.getBoolean("is_mandatory")
        );
    }
}
