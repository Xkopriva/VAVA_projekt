package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku subject_group_translation.
 * Kompozitny PK: (subject_group_id, locale) — bez SERIAL id.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectGroupTranslation {

    private int subjectGroupId; // FK -> subject_group
    private String locale;      // FK -> locale.code, napr. 'sk', 'en'
    private String name;        // lokalizovany nazov skupiny VARCHAR(100)
    private String description; // nullable TEXT
}
