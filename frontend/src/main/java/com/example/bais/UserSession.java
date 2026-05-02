package com.example.bais;

/**
 * Simple session singleton that carries login-time state
 * (role, language, user name) across controllers without a framework.
 */
public class UserSession {

    public enum Role { STUDENT, TEACHER, ADMIN }

    private static UserSession instance;

    private Role    role      = Role.STUDENT;
    private boolean english   = false;
    private String  userEmail;
    private int     userId;
    // Meno načítané z backendu po prihlásení
    private String  firstName = "";
    private String  lastName  = "";

    private UserSession() {}

    public static UserSession get() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public Role    getRole()             { return role; }
    public void    setRole(Role r)       { this.role = r; }
    public boolean isEnglish()           { return english; }
    public void    setEnglish(boolean e) { this.english = e; }
    public boolean isTeacher()           { return role == Role.TEACHER; }
    public boolean isAdmin()             { return role == Role.ADMIN; }

    public String getUserEmail()               { return userEmail; }
    public void   setUserEmail(String email)   { this.userEmail = email; }
    public int    getUserId()                  { return userId; }
    public void   setUserId(int id)            { this.userId = id; }

    public String getFirstName()               { return firstName; }
    public void   setFirstName(String n)       { this.firstName = n == null ? "" : n; }
    public String getLastName()                { return lastName; }
    public void   setLastName(String n)        { this.lastName = n == null ? "" : n; }

    /** Celé meno alebo fallback na email. */
    public String getFullName() {
        String full = (firstName + " " + lastName).trim();
        return full.isEmpty() ? (userEmail != null ? userEmail : "Používateľ") : full;
    }
}
