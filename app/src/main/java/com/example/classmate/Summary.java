package com.example.classmate;

/**
 * מחלקה המייצגת "סיכום לימודי" (כגון קובץ או תמונה).
 * משמשת לניהול המידע אודות הסיכומים שמועלים על ידי המשתמשים לכיתה מסוימת.
 */
public class Summary {
    private String id;            // מזהה ייחודי של הסיכום
    private String title;         // כותרת הסיכום (למשל: "סיכום למבחן בהיסטוריה")
    private String course;        // המקצוע אליו שייך הסיכום (למשל: "היסטוריה")
    private String imageUrl;      // הכתובת (URL) שבה התמונה שמורה בשרת הענן (Firebase Storage)
    private String uploaderEmail; // המייל של המשתמש שהעלה את הסיכום
    private String className;     // הכיתה עבורה הועלה הסיכום
    private long timestamp;       // מתי הועלה הסיכום (תאריך ושעה)

    // בנאי ריק הנדרש עבור Firebase
    public Summary() {}

    // בנאי ליצירת אובייקט סיכום חדש עם כל הפרטים
    public Summary(String id, String title, String course, String imageUrl,
                   String uploaderEmail, String className, long timestamp) {
        this.id = id;
        this.title = title;
        this.course = course;
        this.imageUrl = imageUrl;
        this.uploaderEmail = uploaderEmail;
        this.className = className;
        this.timestamp = timestamp;
    }

    // פונקציות לקבלת ועדכון הנתונים
    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getCourse() { return course != null ? course : ""; }
    public void setCourse(String course) { this.course = course; }

    public String getImageUrl() { return imageUrl != null ? imageUrl : ""; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // פונקציה לשימוש במקרה של תאימות לגרסאות קודמות
    public String getFilePath() { return getImageUrl(); }

    public String getUploaderEmail() { return uploaderEmail != null ? uploaderEmail : ""; }
    public void setUploaderEmail(String uploaderEmail) { this.uploaderEmail = uploaderEmail; }

    public String getClassName() { return className != null ? className : ""; }
    public void setClassName(String className) { this.className = className; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
