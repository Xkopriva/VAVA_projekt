package sk.bais.model;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku "user" v databaze.
 * Student je USER s rolou STUDENT.
 * @Data        = generuje gettery, settery, toString, equals, hashCode aj toString
 * @NoArgsConstructor  = generuje prazdny konstruktor (potrebny pre mapRow v DAO)
 * @AllArgsConstructor = generuje konstruktor so vsetkymi polami
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

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

    // Konstruktor pre INSERT (bez id, createdAt, updatedAt — generuje DB)
    public Student(String email, String firstName, String lastName,
                   String passwordHash, boolean isActive, String profilePictureUrl) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.isActive = isActive;
        this.profilePictureUrl = profilePictureUrl;
    }

}