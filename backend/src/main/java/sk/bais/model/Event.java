package sk.bais.model;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku event.
 * Reprezentuje naplanovanu udalost predmetu (prednaska, cvicenie, skuska, ...).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    public enum Type {
        PREDNASKA, CVICENIE, ZAPOCET, ODOVZDANIE, TASK, EXAM, PISOMKA
    }

    private int id;
    private int subjectId;              // FK -> subject NOT NULL
    private Type type;                  // PREDNASKA | CVICENIE | ... NOT NULL
    private Integer weekNumber;         // nullable
    private String room;                // nullable VARCHAR(100)
    private OffsetDateTime scheduledAt; // nullable TIMESTAMPTZ
    private Integer durationMinutes;    // nullable
    @JsonProperty("isPublished")
    private boolean isPublished;        // DEFAULT FALSE
    private Integer createdBy;          // FK -> user nullable
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Konstruktor pre INSERT — povinne polia
    public Event(int subjectId, Type type) {
        this.subjectId = subjectId;
        this.type = type;
    }
}
