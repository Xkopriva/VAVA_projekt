package sk.bais.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku index_record.
 * Reprezentuje oficialnu znamku za jeden enrollment.
 *
 * final_mark: A | B | C | D | E | FX | PASS | FAIL
 */
public class IndexRecord {

    private int id;
    private int enrollmentId;   // FK -> enrollment (UNIQUE — jeden zaznam na enrollment)
    private Integer recordedBy; // FK -> user (nullable)
    private String finalMark;   // A/B/C/D/E/FX/PASS/FAIL
    private OffsetDateTime recordedAt;
    private LocalDate examDate; // DATE, nullable
    private String notes;

    public IndexRecord() {}

    // Konstruktor pre INSERT
    public IndexRecord(int enrollmentId, Integer recordedBy,
                       String finalMark, LocalDate examDate, String notes) {
        this.enrollmentId = enrollmentId;
        this.recordedBy = recordedBy;
        this.finalMark = finalMark;
        this.examDate = examDate;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int v) { this.enrollmentId = v; }

    public Integer getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Integer v) { this.recordedBy = v; }

    public String getFinalMark() { return finalMark; }
    public void setFinalMark(String v) { this.finalMark = v; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime v) { this.recordedAt = v; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate v) { this.examDate = v; }

    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }

    @Override
    public String toString() {
        return "IndexRecord{id=" + id + ", enrollmentId=" + enrollmentId +
               ", finalMark='" + finalMark + "', examDate=" + examDate + "}";
    }
}