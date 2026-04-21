package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku subject_translation.
 * Kompozitny PK: (subject_id, locale) — bez SERIAL id.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectTranslation {

    private int subjectId;      // FK -> subject
    private String locale;      // FK -> locale.code, napr. 'sk', 'en'
    private String name;        // lokalizovany nazov predmetu VARCHAR(255)
    private String description; // plny syllabus v danom locale (nullable TEXT)
}
