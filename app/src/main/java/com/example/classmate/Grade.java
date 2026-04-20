package com.example.classmate;

/**
 * מחלקה המייצגת "ציון" של תלמיד.
 * משמשת לשמירת פרטי המבחן, התלמיד והציון שקיבל בבסיס הנתונים.
 */
public class Grade {
    private String id;          // מזהה ייחודי של הציון
    private String studentId;   // מזהה התלמיד (UID) שקיבל את הציון
    private String studentName; // שם התלמיד
    private String subject;     // המקצוע (למשל: מתמטיקה)
    private int score;          // הציון שקיבל (מספר)
    private long timestamp;     // מתי הציון הוזן/בוצע המבחן
    private String className;   // שם הכיתה

    // בנאי ריק הנדרש עבור Firebase
    public Grade() {}

    // בנאי ליצירת אובייקט ציון חדש
    public Grade(String id, String studentId, String studentName, String subject, int score, long timestamp, String className) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.subject = subject;
        this.score = score;
        this.timestamp = timestamp;
        this.className = className;
    }

    // פונקציות לקריאה ועדכון של הנתונים
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}
