package com.example.bais;

/**
 * Simple session singleton that carries login-time state
 * (role, language) across controllers without a framework.
 */
public class UserSession {

    public enum Role { STUDENT, TEACHER, ADMIN }

    private static UserSession instance;

    private Role    role     = Role.STUDENT;
    private boolean english  = false;
    private String  userEmail;
    private int     userId;

    private UserSession() {}

    public static UserSession get() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public Role    getRole()           { return role; }
    public void    setRole(Role r)     { this.role = r; }
    public boolean isEnglish()         { return english; }
    public void    setEnglish(boolean e){ this.english = e; }
    public boolean isTeacher()         { return role == Role.TEACHER; }
    public boolean isAdmin()           { return role == Role.ADMIN; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}
