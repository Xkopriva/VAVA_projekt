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
    private int subjectId; 
    private Type type; 
    private Integer weekNumber;
    private String room;
    private OffsetDateTime scheduledAt;
    private Integer durationMinutes; 
    @JsonProperty("isPublished")
    private boolean isPublished; 
    private Integer createdBy; 
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Konstruktor pre INSERT — povinne polia
    public Event(int subjectId, Type type) {
        this.subjectId = subjectId;
        this.type = type;
    }
}
