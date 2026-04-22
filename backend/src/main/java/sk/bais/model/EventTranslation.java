package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku event_translation.
 * Kompozitny PK: (event_id, locale) — bez SERIAL id.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventTranslation {

    private int eventId;        // FK -> event
    private String locale;      // FK -> locale.code, napr. 'sk', 'en'
    private String title;       // lokalizovany nazov udalosti VARCHAR(255)
    private String description; // nullable TEXT
}
