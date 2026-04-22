package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku permission.
 * Reprezentuje jemnozrnnne opravnenie, napr. 'subjects:create'.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    private int id;
    private String name;        // VARCHAR(100) UNIQUE NOT NULL, napr. 'subjects:write'
    private String resource;    // VARCHAR(50) NOT NULL, napr. 'subjects'
    private String action;      // VARCHAR(50) NOT NULL, napr. 'WRITE'

    // Konstruktor pre INSERT
    public Permission(String name, String resource, String action) {
        this.name = name;
        this.resource = resource;
        this.action = action;
    }
}
