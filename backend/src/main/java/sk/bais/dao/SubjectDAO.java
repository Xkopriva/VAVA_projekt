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

import sk.bais.model.Subject;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku subject.
 * completion_type je VARCHAR s CHECK — posielame ho ako obycajny String, bez ::cast.
 */
public class SubjectDAO {

    private static final Logger log = LoggerFactory.getLogger(SubjectDAO.class);

    private static final String COLS =
            "id, code, external_id, faculty, credits, is_mandatory, is_profiled, " +
            "completion_type, lecture_hrs_weekly, lab_hrs_weekly, " +
            "seminar_hrs_weekly, project_hrs_weekly, " +
            "language_of_instruction, assessment_breakdown, recommended_semester, " +
            "guarantor_id, avg_student_rating, subject_difficulty, " +
            "total_assessed_students, " +
            "grade_a_pct, grade_b_pct, grade_c_pct, " +
            "grade_d_pct, grade_e_pct, grade_fx_pct, " +
            "last_modified, created_by, created_at, updated_at";

    private static final String SQL_LIST =
            "SELECT " + COLS + " FROM subject ORDER BY id";

    private static final String SQL_GET_BY_ID =
            "SELECT " + COLS + " FROM subject WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO subject " +
            "(code, credits, is_mandatory, is_profiled, completion_type, " +
            " lecture_hrs_weekly, lab_hrs_weekly, seminar_hrs_weekly, project_hrs_weekly, " +
            " language_of_instruction, assessment_breakdown, recommended_semester, " +
            " guarantor_id, created_by, faculty) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE subject SET " +
            "code = ?, credits = ?, is_mandatory = ?, is_profiled = ?, completion_type = ?, " +
            "lecture_hrs_weekly = ?, lab_hrs_weekly = ?, seminar_hrs_weekly = ?, project_hrs_weekly = ?, " +
            "language_of_instruction = ?, assessment_breakdown = ?, recommended_semester = ?, " +
            "guarantor_id = ?, faculty = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM subject WHERE id = ?";

    //  LIST 
    public List<Subject> list() throws SQLException {
        List<Subject> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} subject zaznamov", list.size());
        return list;
    }

    //  GET BY ID 
    public Optional<Subject> getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdeny subject id={}", id);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("Subject id={} nenajdeny", id);
        return Optional.empty();
    }

    //  CREATE 
    public Subject create(Subject s) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, s.getCode());
            stmt.setInt(2, s.getCredits());
            stmt.setBoolean(3, s.isMandatory());
            stmt.setBoolean(4, s.isProfiled());
            // completion_type je VARCHAR — posielame priamo ako String, DB CHECK to overi
            stmt.setString(5, s.getCompletionType().name());
            setNullableInt(stmt, 6, s.getLectureHrsWeekly());
            setNullableInt(stmt, 7, s.getLabHrsWeekly());
            setNullableInt(stmt, 8, s.getSeminarHrsWeekly());
            setNullableInt(stmt, 9, s.getProjectHrsWeekly());
            stmt.setString(10, s.getLanguageOfInstruction());
            stmt.setString(11, s.getAssessmentBreakdown());
            setNullableInt(stmt, 12, s.getRecommendedSemester());
            setNullableInt(stmt, 13, s.getGuarantorId());
            setNullableInt(stmt, 14, s.getCreatedBy());
            stmt.setString(15, s.getFaculty());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
        }
        log.info("Vytvoreny subject id={}", s.getId());
        return s;
    }

    //  UPDATE 
    public boolean update(Subject s) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, s.getCode());
            stmt.setInt(2, s.getCredits());
            stmt.setBoolean(3, s.isMandatory());
            stmt.setBoolean(4, s.isProfiled());
            stmt.setString(5, s.getCompletionType().name());
            setNullableInt(stmt, 6, s.getLectureHrsWeekly());
            setNullableInt(stmt, 7, s.getLabHrsWeekly());
            setNullableInt(stmt, 8, s.getSeminarHrsWeekly());
            setNullableInt(stmt, 9, s.getProjectHrsWeekly());
            stmt.setString(10, s.getLanguageOfInstruction());
            stmt.setString(11, s.getAssessmentBreakdown());
            setNullableInt(stmt, 12, s.getRecommendedSemester());
            setNullableInt(stmt, 13, s.getGuarantorId());
            stmt.setString(14, s.getFaculty());
            stmt.setInt(15, s.getId());

            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Subject id={} uspesne upraveny", s.getId());
            return updated;
        }
    }

    //  DELETE 
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazany subject id={}", id);
            return deleted;
        }
    }

    //  MAPPER 
    private Subject mapRow(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setId(rs.getInt("id"));
        s.setCode(rs.getString("code"));
        setIfNotNull(rs, "external_id", v -> s.setExternalId(v));
        s.setFaculty(rs.getString("faculty"));
        s.setCredits(rs.getInt("credits"));
        s.setMandatory(rs.getBoolean("is_mandatory"));
        s.setProfiled(rs.getBoolean("is_profiled"));

        String ct = rs.getString("completion_type");
        if (ct != null) s.setCompletionType(Subject.CompletionType.valueOf(ct));

        setIfNotNull(rs, "lecture_hrs_weekly",  v -> s.setLectureHrsWeekly(v));
        setIfNotNull(rs, "lab_hrs_weekly",       v -> s.setLabHrsWeekly(v));
        setIfNotNull(rs, "seminar_hrs_weekly",   v -> s.setSeminarHrsWeekly(v));
        setIfNotNull(rs, "project_hrs_weekly",   v -> s.setProjectHrsWeekly(v));
        s.setLanguageOfInstruction(rs.getString("language_of_instruction"));
        s.setAssessmentBreakdown(rs.getString("assessment_breakdown"));
        setIfNotNull(rs, "recommended_semester", v -> s.setRecommendedSemester(v));
        setIfNotNull(rs, "guarantor_id",         v -> s.setGuarantorId(v));
        s.setAvgStudentRating(rs.getBigDecimal("avg_student_rating"));
        s.setSubjectDifficulty(rs.getBigDecimal("subject_difficulty"));
        setIfNotNull(rs, "total_assessed_students", v -> s.setTotalAssessedStudents(v));
        s.setGradeAPct(rs.getBigDecimal("grade_a_pct"));
        s.setGradeBPct(rs.getBigDecimal("grade_b_pct"));
        s.setGradeCPct(rs.getBigDecimal("grade_c_pct"));
        s.setGradeDPct(rs.getBigDecimal("grade_d_pct"));
        s.setGradeEPct(rs.getBigDecimal("grade_e_pct"));
        s.setGradeFxPct(rs.getBigDecimal("grade_fx_pct"));

        java.sql.Date ld = rs.getDate("last_modified");
        if (ld != null) s.setLastModified(ld.toLocalDate());

        setIfNotNull(rs, "created_by", v -> s.setCreatedBy(v));
        s.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        s.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return s;
    }

    // Helper pre nullable INT polia
    private void setNullableInt(PreparedStatement stmt, int idx, Integer val) throws SQLException {
        if (val != null) stmt.setInt(idx, val);
        else stmt.setNull(idx, Types.INTEGER);
    }

    // Helper pre citanie nullable INT z ResultSet pomocou lambdy
    @FunctionalInterface
    interface IntSetter { void set(Integer v); }

    private void setIfNotNull(ResultSet rs, String col, IntSetter setter) throws SQLException {
        int v = rs.getInt(col);
        if (!rs.wasNull()) setter.set(v);
    }
}