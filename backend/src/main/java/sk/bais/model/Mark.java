package sk.bais.students;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku mark.
 * Reprezentuje jednotlive hodnotenie (domaca uloha, test) v ramci enrollment.
 */
public class Mark {

    private int id;
    private int enrollmentId;
    private Integer eventId;            // nullable FK -> event (pridany v V005)
    private Integer taskSubmissionId;   // nullable FK -> task_submission (pridany v V005)
    private String title;               // VARCHAR(255) — pisany ucitelom, nelokalizovany
    private BigDecimal points;          // DECIMAL(6,2)
    private BigDecimal maxPoints;       // DECIMAL(6,2)
    private Integer givenBy;            // nullable FK -> user
    private OffsetDateTime givenAt;
    private String notes;

    public Mark() {}

    // Konstruktor pre INSERT
    public Mark(int enrollmentId, String title,
                BigDecimal points, BigDecimal maxPoints, Integer givenBy) {
        this.enrollmentId = enrollmentId;
        this.title = title;
        this.points = points;
        this.maxPoints = maxPoints;
        this.givenBy = givenBy;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int v) { this.enrollmentId = v; }

    public Integer getEventId() { return eventId; }
    public void setEventId(Integer v) { this.eventId = v; }

    public Integer getTaskSubmissionId() { return taskSubmissionId; }
    public void setTaskSubmissionId(Integer v) { this.taskSubmissionId = v; }

    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }

    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal v) { this.points = v; }

    public BigDecimal getMaxPoints() { return maxPoints; }
    public void setMaxPoints(BigDecimal v) { this.maxPoints = v; }

    public Integer getGivenBy() { return givenBy; }
    public void setGivenBy(Integer v) { this.givenBy = v; }

    public OffsetDateTime getGivenAt() { return givenAt; }
    public void setGivenAt(OffsetDateTime v) { this.givenAt = v; }

    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }

    @Override
    public String toString() {
        return "Mark{id=" + id + ", enrollmentId=" + enrollmentId +
               ", title='" + title + "', points=" + points +
               "/" + maxPoints + "}";
    }
}