package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku locale.
 * PK je code (BCP-47 tag, napr. 'sk', 'en') — nie je SERIAL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Locale {

    private String code;        // PK VARCHAR(10), napr. 'sk', 'en'
    private String name;        // VARCHAR(50), napr. 'Slovenčina'
    private boolean isDefault;  // prave jedna polozka ma TRUE
}
