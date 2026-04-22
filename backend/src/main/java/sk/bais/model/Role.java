package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku role.
 * Reprezentuje aplikacnu rolu (STUDENT, TEACHER, POWER_USER, ADMIN).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    private int id;
    private String name;            // VARCHAR(50) UNIQUE NOT NULL, napr. 'STUDENT'
    private String description;     // nullable TEXT
    private OffsetDateTime createdAt;

    // Konstruktor pre INSERT
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
