package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku semester.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Semester {

    public enum Type {
        WINTER, SUMMER
    }

    public enum Status {
        PLANNED, ACTIVE, FINISHED
    }

    private int id;
    private String code;            // WS_2024_2025
    private Type type;              // WINTER / SUMMER
    private String academicYear;    // 2024/2025
    private LocalDate startDate;
    private LocalDate endDate;
    private Status status;          // PLANNED / ACTIVE / FINISHED
    private OffsetDateTime createdAt;

    /**
     * Konstruktor pre INSERT.
     */
    public Semester(String code, Type type, String academicYear, Status status) {
        this.code = code;
        this.type = type;
        this.academicYear = academicYear;
        this.status = status;
    }
}
