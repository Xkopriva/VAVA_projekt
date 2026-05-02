package com.example.bais;

public class UserProfile {
    private String fullName;
    private String studentId;
    private String email;
    private String phone;
    private String homeLanguage;

    // Constructor
    public UserProfile(String fullName, String studentId, String email, String phone, String homeLanguage) {
        this.fullName = fullName;
        this.studentId = studentId;
        this.email = email;
        this.phone = phone;
        this.homeLanguage = homeLanguage;
    }

    // Getters
    public String getFullName() {
        return fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getHomeLanguage() {
        return homeLanguage;
    }

    // Setters (if needed, for editable fields)
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setHomeLanguage(String homeLanguage) {
        this.homeLanguage = homeLanguage;
    }
}
