package com.example.classmate;

/**
 * מחלקה המייצגת "תלמיד" באפליקציה.
 * משמשת לשמירת פרטי המשתמש (שם, אימייל, כיתה) וניהולם בממשק.
 */
public class Student {
    private String fullName;  // השם המלא של התלמיד
    private String email;     // כתובת האימייל של התלמיד
    private String userId;    // המזהה הייחודי של המשתמש במערכת (UID)
    private String className; // שם הכיתה אליה התלמיד משתייך
    private boolean admin;    // האם המשתמש הוא מנהל (אמת/שקר)

    // בנאי ריק הנדרש עבור Firebase
    public Student() {}

    // בנאי ליצירת אובייקט תלמיד חדש עם כל הפרטים
    public Student(String fullName, String email, String userId, String className, boolean admin) {
        this.fullName = fullName;
        this.email = email;
        this.userId = userId;
        this.className = className;
        this.admin = admin;
    }

    // פונקציות המאפשרות לקבל ולעדכן את נתוני התלמיד בבטחה
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
