package sk.bais.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku "user".
 * Reprezentuje lubovolneho pouzivatela systemu bez ohadu na rolu.
 * Pre role-specificke operacie pouzi Student/Teacher/Admin/PowerUser.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private int id;
    private String email;               // VARCHAR(255) UNIQUE NOT NULL
    private String firstName;           // VARCHAR(100) NOT NULL
    private String lastName;            // VARCHAR(100) NOT NULL
    private String passwordHash;        // VARCHAR(255) NOT NULL — nikdy nezobrazovat!
    private boolean isActive;           // DEFAULT TRUE
    private String profilePictureUrl;   // nullable VARCHAR(500)
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Konstruktor pre INSERT — bez id, createdAt, updatedAt (generuje DB)
    public User(String email, String firstName, String lastName,
                String passwordHash, boolean isActive, String profilePictureUrl) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.isActive = isActive;
        this.profilePictureUrl = profilePictureUrl;
    }
}
