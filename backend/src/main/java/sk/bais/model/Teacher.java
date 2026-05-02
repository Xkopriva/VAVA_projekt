package sk.bais.model;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku "user".
 * Teacher je USER s rolou TEACHER.
 * @Data        = generuje gettery, settery, toString, equals, hashCode aj toString
 * @NoArgsConstructor  = generuje prazdny konstruktor (potrebny pre mapRow v DAO)
 * @AllArgsConstructor = generuje konstruktor so vsetkymi polami
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {

    private int id;
    private String email;
    private String firstName;
    private String lastName;
    private String passwordHash;
    @JsonProperty("isActive")
    private boolean isActive;
    private String profilePictureUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Teacher(String email, String firstName, String lastName,
                   String passwordHash, boolean isActive, String profilePictureUrl) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.isActive = isActive;
        this.profilePictureUrl = profilePictureUrl;
    }
}