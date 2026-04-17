package sk.bais.model;

import java.time.OffsetDateTime;

/**
 * Model trieda mapovana na tabulku "user" v databaze.
 * Student je USER s rolou STUDENT.
 */
public class Student {

    private int id;
    private String email;
    private String firstName;
    private String lastName;
    private String passwordHash;
    private boolean isActive;
    private String profilePictureUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Student() {}

    // Konstruktor pre nacitanie z DB (vsetky polia)
    public Student(int id, String email, String firstName, String lastName,
                   String passwordHash, boolean isActive, String profilePictureUrl,
                   OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.isActive = isActive;
        this.profilePictureUrl = profilePictureUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    // --- Getters & Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}