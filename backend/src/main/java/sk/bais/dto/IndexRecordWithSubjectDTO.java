package sk.bais.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Obohacený IndexRecord – spája Known index_record + predmet.
 * Posiela sa frontendu ako odpoveď na GET_MY_MARKS.
 */
public class IndexRecordWithSubjectDTO {

    private int id;
    private int enrollmentId;
    private Integer recordedBy;
    private String finalMark;
    private OffsetDateTime recordedAt;
    private LocalDate examDate;
    private String notes;

    // Polia predmetu (JOIN)
    private String subjectCode;
    private String subjectName;
    private int credits;

    public IndexRecordWithSubjectDTO() {}

    public IndexRecordWithSubjectDTO(
            int id, int enrollmentId, Integer recordedBy, String finalMark,
            OffsetDateTime recordedAt, LocalDate examDate, String notes,
            String subjectCode, String subjectName, int credits) {
        this.id = id;
        this.enrollmentId = enrollmentId;
        this.recordedBy = recordedBy;
        this.finalMark = finalMark;
        this.recordedAt = recordedAt;
        this.examDate = examDate;
        this.notes = notes;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.credits = credits;
    }

    // Gettery (Jackson ich potrebuje na serializáciu)

    public int getId() { return id; }
    public int getEnrollmentId() { return enrollmentId; }
    public Integer getRecordedBy() { return recordedBy; }
    public String getFinalMark() { return finalMark; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public LocalDate getExamDate() { return examDate; }
    public String getNotes() { return notes; }
    public String getSubjectCode() { return subjectCode; }
    public String getSubjectName() { return subjectName; }
    public int getCredits() { return credits; }
}

