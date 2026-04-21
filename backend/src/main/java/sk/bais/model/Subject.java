package sk.bais.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku subject.
 * @Data        = generuje gettery, settery, toString, equals, hashCode aj toString
 * @NoArgsConstructor  = generuje prazdny konstruktor (potrebny pre mapRow v DAO)
 * @AllArgsConstructor = generuje konstruktor so vsetkymi polami
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    // Konstruktor pre INSERT — len povinne polia
    public Subject(String code, int credits, boolean isMandatory,
                   CompletionType completionType, Integer guarantorId) {
        this.code = code;
        this.credits = credits;
        this.isMandatory = isMandatory;
        this.completionType = completionType;
        this.guarantorId = guarantorId;
    }
}