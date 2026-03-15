package com.example.classmate;

public class Student {
    private String fullName;
    private String email;
    private String userId;
    private String className;
    private boolean admin;

    public Student() {}

    public Student(String fullName, String email, String userId, String className, boolean admin) {
        this.fullName = fullName;
        this.email = email;
        this.userId = userId;
        this.className = className;
        this.admin = admin;
    }

    public String getFullName() { return fullName != null ? fullName : ""; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email; }

    public String getUserId() { return userId != null ? userId : ""; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getClassName() { return className != null ? className : ""; }
    public void setClassName(String className) { this.className = className; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
}
