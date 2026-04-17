package sk.bais.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku subject.
 */
public class Subject {

    public enum CompletionType {
        EXAM, GRADED_CREDIT, PASS_CREDIT
    }

    private int id;
    private String code;                    // VARCHAR(20) UNIQUE NOT NULL
    private Integer externalId;
    private String faculty;                 // VARCHAR(20)
    private int credits;                    // INT NOT NULL
    private boolean isMandatory;
    private boolean isProfiled;             // is_profiled
    private CompletionType completionType;
    private Integer lectureHrsWeekly;
    private Integer labHrsWeekly;
    private Integer seminarHrsWeekly;
    private Integer projectHrsWeekly;
    private String languageOfInstruction;
    private String assessmentBreakdown;
    private Integer recommendedSemester;
    private Integer guarantorId;            // FK -> user
    private BigDecimal avgStudentRating;    // DECIMAL(2,1)
    private BigDecimal subjectDifficulty;   // DECIMAL(2,1)
    private Integer totalAssessedStudents;
    private BigDecimal gradeAPct;
    private BigDecimal gradeBPct;
    private BigDecimal gradeCPct;
    private BigDecimal gradeDPct;
    private BigDecimal gradeEPct;
    private BigDecimal gradeFxPct;
    private LocalDate lastModified;         // DATE
    private Integer createdBy;              // FK -> user
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Subject() {}

    // Konstruktor pre INSERT — len povinne polia
    public Subject(String code, int credits, boolean isMandatory,
                   CompletionType completionType, Integer guarantorId) {
        this.code = code;
        this.credits = credits;
        this.isMandatory = isMandatory;
        this.completionType = completionType;
        this.guarantorId = guarantorId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getExternalId() { return externalId; }
    public void setExternalId(Integer v) { this.externalId = v; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public boolean isMandatory() { return isMandatory; }
    public void setMandatory(boolean v) { isMandatory = v; }

    public boolean isProfiled() { return isProfiled; }
    public void setProfiled(boolean v) { isProfiled = v; }

    public CompletionType getCompletionType() { return completionType; }
    public void setCompletionType(CompletionType v) { this.completionType = v; }

    public Integer getLectureHrsWeekly() { return lectureHrsWeekly; }
    public void setLectureHrsWeekly(Integer v) { this.lectureHrsWeekly = v; }

    public Integer getLabHrsWeekly() { return labHrsWeekly; }
    public void setLabHrsWeekly(Integer v) { this.labHrsWeekly = v; }

    public Integer getSeminarHrsWeekly() { return seminarHrsWeekly; }
    public void setSeminarHrsWeekly(Integer v) { this.seminarHrsWeekly = v; }

    public Integer getProjectHrsWeekly() { return projectHrsWeekly; }
    public void setProjectHrsWeekly(Integer v) { this.projectHrsWeekly = v; }

    public String getLanguageOfInstruction() { return languageOfInstruction; }
    public void setLanguageOfInstruction(String v) { this.languageOfInstruction = v; }

    public String getAssessmentBreakdown() { return assessmentBreakdown; }
    public void setAssessmentBreakdown(String v) { this.assessmentBreakdown = v; }

    public Integer getRecommendedSemester() { return recommendedSemester; }
    public void setRecommendedSemester(Integer v) { this.recommendedSemester = v; }

    public Integer getGuarantorId() { return guarantorId; }
    public void setGuarantorId(Integer v) { this.guarantorId = v; }

    public BigDecimal getAvgStudentRating() { return avgStudentRating; }
    public void setAvgStudentRating(BigDecimal v) { this.avgStudentRating = v; }

    public BigDecimal getSubjectDifficulty() { return subjectDifficulty; }
    public void setSubjectDifficulty(BigDecimal v) { this.subjectDifficulty = v; }

    public Integer getTotalAssessedStudents() { return totalAssessedStudents; }
    public void setTotalAssessedStudents(Integer v) { this.totalAssessedStudents = v; }

    public BigDecimal getGradeAPct() { return gradeAPct; }
    public void setGradeAPct(BigDecimal v) { this.gradeAPct = v; }

    public BigDecimal getGradeBPct() { return gradeBPct; }
    public void setGradeBPct(BigDecimal v) { this.gradeBPct = v; }

    public BigDecimal getGradeCPct() { return gradeCPct; }
    public void setGradeCPct(BigDecimal v) { this.gradeCPct = v; }

    public BigDecimal getGradeDPct() { return gradeDPct; }
    public void setGradeDPct(BigDecimal v) { this.gradeDPct = v; }

    public BigDecimal getGradeEPct() { return gradeEPct; }
    public void setGradeEPct(BigDecimal v) { this.gradeEPct = v; }

    public BigDecimal getGradeFxPct() { return gradeFxPct; }
    public void setGradeFxPct(BigDecimal v) { this.gradeFxPct = v; }

    public LocalDate getLastModified() { return lastModified; }
    public void setLastModified(LocalDate v) { this.lastModified = v; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer v) { this.createdBy = v; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }

    @Override
    public String toString() {
        return "Subject{id=" + id + ", code='" + code + "', credits=" + credits +
               ", completionType=" + completionType + ", faculty='" + faculty + "'}";
    }
}