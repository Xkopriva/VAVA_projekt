package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku subject_group.
 * Reprezentuje skupinu predmetov v ramci semestra (povinne, povinne volitelne, atd.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectGroup {

    private int id;
    private int semesterId;     // FK -> semester
    private int sortOrder;      // poradie zoradenia
    private OffsetDateTime createdAt;

    // Konstruktor pre INSERT
    public SubjectGroup(int semesterId, int sortOrder) {
        this.semesterId = semesterId;
        this.sortOrder = sortOrder;
    }
}
