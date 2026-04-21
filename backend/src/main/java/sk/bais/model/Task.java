package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku task.
 * Reprezentuje zadanie vytvorene ucitelom — title/description NIE su lokalizovane.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    private int id;
    private Integer eventId;        // FK -> event nullable
    private int subjectId;          // FK -> subject NOT NULL
    private String title;           // VARCHAR(255) NOT NULL — v jazyku ucitela
    private String description;     // nullable TEXT
    private OffsetDateTime dueAt;   // nullable TIMESTAMPTZ
    private BigDecimal maxPoints;   // nullable DECIMAL(6,2)
    private boolean isPublished;    // DEFAULT FALSE
    private Integer createdBy;      // FK -> user nullable
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Konstruktor pre INSERT — povinne polia
    public Task(int subjectId, String title) {
        this.subjectId = subjectId;
        this.title = title;
    }
}
