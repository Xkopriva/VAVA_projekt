package sk.bais.model;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku notification.
 * Obsah (title/message) je generovany v locale prijaleca v case odoslania — nie je lokalizovany dodatocne.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    public enum Type {
        MARK_ADDED, TASK_DUE, ENROLLMENT_OPEN,
        ANNOUNCEMENT, EXAM_SCHEDULED, SUBMISSION_GRADED
    }

    private int id;
    private int recipientId;            // FK -> user NOT NULL
    private Integer senderId;           // FK -> user nullable
    private Type type;                  // enum NOT NULL
    private String title;               // VARCHAR(255) NOT NULL
    private String message;             // nullable TEXT
    @JsonProperty("isRead")
    private boolean isRead;             // DEFAULT FALSE
    private Integer relatedMarkId;      // FK -> mark nullable
    private Integer relatedSubjectId;   // FK -> subject nullable
    private Integer relatedTaskId;      // FK -> task nullable
    private OffsetDateTime createdAt;
    private OffsetDateTime readAt;      // nullable — set when is_read = TRUE

    // Konstruktor pre INSERT — povinne polia
    public Notification(int recipientId, Type type, String title) {
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
    }
}
