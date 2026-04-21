package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku semester_translation.
 * Kompozitny PK: (semester_id, locale) — bez SERIAL id.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemesterTranslation {

    private int semesterId;     // FK -> semester
    private String locale;      // FK -> locale.code, napr. 'sk', 'en'
    private String name;        // lokalizovany nazov semestra
}
