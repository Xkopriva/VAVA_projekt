package sk.bais.dto;

/**
 * Profil prihláseného používateľa – odpoveď na GET_USER_PROFILE.
 */
public class UserProfileDTO {

    private int    userId;
    private String email;
    private String firstName;
    private String lastName;

    public UserProfileDTO() {}

    public UserProfileDTO(int userId, String email, String firstName, String lastName) {
        this.userId    = userId;
        this.email     = email;
        this.firstName = firstName;
        this.lastName  = lastName;
    }

    public int    getUserId()    { return userId; }
    public String getEmail()     { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
}
