package sk.bais.students;

import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku enrollment.
 * Reprezentuje zapis studenta na predmet v danom semestri.
 *
 * status: ACTIVE | PASSED | FAILED | WITHDRAWN
 */
public class Enrollment {

    public enum Status {
        ACTIVE, PASSED, FAILED, WITHDRAWN
    }

    private int id;
    private int studentId;      // FK -> user (student)
    private int subjectId;      // FK -> subject
    private int semesterId;     // FK -> semester
    private int attemptNumber;  // 1 = prvy pokus, 2 = opakovany
    private OffsetDateTime enrolledAt;
    private Status status;

    public Enrollment() {}

    // Konstruktor pre INSERT
    public Enrollment(int studentId, int subjectId, int semesterId,
                      int attemptNumber, Status status) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.semesterId = semesterId;
        this.attemptNumber = attemptNumber;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int v) { this.studentId = v; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int v) { this.subjectId = v; }

    public int getSemesterId() { return semesterId; }
    public void setSemesterId(int v) { this.semesterId = v; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int v) { this.attemptNumber = v; }

    public OffsetDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(OffsetDateTime v) { this.enrolledAt = v; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Override
    public String toString() {
        return "Enrollment{id=" + id + ", studentId=" + studentId +
               ", subjectId=" + subjectId + ", semesterId=" + semesterId +
               ", attempt=" + attemptNumber + ", status=" + status + "}";
    }
}