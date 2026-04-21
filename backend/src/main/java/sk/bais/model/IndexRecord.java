package sk.bais.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku index_record.
 * Reprezentuje oficialnu znamku za jeden enrollment.
 *
 * final_mark: A | B | C | D | E | FX | PASS | FAIL
 * @Data        = generuje gettery, settery, toString, equals, hashCode aj toString
 * @NoArgsConstructor  = generuje prazdny konstruktor (potrebny pre mapRow v DAO)
 * @AllArgsConstructor = generuje konstruktor so vsetkymi polami
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexRecord {

    private int id;
    private int enrollmentId;   // FK -> enrollment (UNIQUE — jeden zaznam na enrollment)
    private Integer recordedBy; // FK -> user (nullable)
    private String finalMark;   // A/B/C/D/E/FX/PASS/FAIL
    private OffsetDateTime recordedAt;
    private LocalDate examDate; // DATE, nullable
    private String notes;

    // Konstruktor pre INSERT
    public IndexRecord(int enrollmentId, Integer recordedBy,
                       String finalMark, LocalDate examDate, String notes) {
        this.enrollmentId = enrollmentId;
        this.recordedBy = recordedBy;
        this.finalMark = finalMark;
        this.examDate = examDate;
        this.notes = notes;
    }
}