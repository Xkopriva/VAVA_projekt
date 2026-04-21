package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku task_submission.
 * Reprezentuje odovzdanie zadania studentom.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmission {

    public enum Status {
        PENDING, SUBMITTED, GRADED, LATE, MISSING
    }

    private int id;
    private int taskId;                 // FK -> task NOT NULL
    private int studentId;              // FK -> user NOT NULL
    private OffsetDateTime submittedAt; // nullable — moze byt NULL pre PENDING
    private String content;             // nullable TEXT
    private String fileUrl;             // nullable VARCHAR(500)
    private Status status;              // PENDING | SUBMITTED | GRADED | LATE | MISSING
    private Integer gradedBy;           // FK -> user nullable
    private OffsetDateTime gradedAt;    // nullable

    // Konstruktor pre INSERT — povinne polia
    public TaskSubmission(int taskId, int studentId, Status status) {
        this.taskId = taskId;
        this.studentId = studentId;
        this.status = status;
    }
}
