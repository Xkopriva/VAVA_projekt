package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku enrollment.
 *
 * @Data        = generuje gettery, settery, toString, equals, hashCode aj toString
 * @NoArgsConstructor  = generuje prazdny konstruktor (potrebny pre mapRow v DAO)
 * @AllArgsConstructor = generuje konstruktor so vsetkymi polami
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    public enum Status {
        ACTIVE, PASSED, FAILED, WITHDRAWN
    }

    private int id;
    private int studentId;
    private int subjectId;
    private int semesterId;
    private int attemptNumber;
    private OffsetDateTime enrolledAt;
    private Status status;

    /**
     * Konstruktor pre INSERT — bez id a enrolledAt (generuje DB).
     * @Data negeneruje tento specificky konstruktor automaticky,
     * preto ho definujeme manualne.
     */
    public Enrollment(int studentId, int subjectId, int semesterId,
                      int attemptNumber, Status status) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.semesterId = semesterId;
        this.attemptNumber = attemptNumber;
        this.status = status;
    }
}