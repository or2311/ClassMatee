package com.example.classmate;

/**
 * מחלקה זו מייצגת "אירוע" באפליקציה (כמו מבחן, בוחן או תזכורת).
 * היא משמשת כ"תבנית" ששומרת את כל הפרטים של אירוע מסוים.
 */
public class Event {
    private String id;          // מזהה ייחודי של האירוע בבסיס הנתונים
    private String subject;     // המקצוע (למשל: מתמטיקה)
    private String type;        // סוג האירוע (למשל: מבחן, בוחן)
    private long timestamp;     // תאריך וזמן בפורמט מספר (כדי שהמחשב יוכל למיין בקלות)
    private String material;    // החומר למבחן
    private String notes;       // הערות נוספות
    private String className;   // לאיזו כיתה האירוע שייך
    private String createdBy;   // מי יצר את האירוע (המזהה של המשתמש)
    private boolean isGlobal;   // האם זה אירוע שכולם רואים (שיצר מנהל) או אירוע פרטי

    // בנאי (Constructor) ריק - נדרש עבור Firebase
    public Event() {}

    // בנאי ליצירת אירוע חדש עם כל הפרטים
    public Event(String id, String subject, String type, long timestamp,
                 String material, String notes, String className, String createdBy, boolean isGlobal) {
        this.id = id;
        this.subject = subject;
        this.type = type;
        this.timestamp = timestamp;
        this.material = material;
        this.notes = notes;
        this.className = className;
        this.createdBy = createdBy;
        this.isGlobal = isGlobal;
    }

    // פונקציות "גטרים" (Getters) ו"סטרים" (Setters) - מאפשרות לקרוא ולעדכן את הנתונים בבטחה
    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getSubject() { return subject != null ? subject : ""; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getType() { return type != null ? type : "מבחן"; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getClassName() { return className != null ? className : ""; }
    public void setClassName(String className) { this.className = className; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public boolean isGlobal() { return isGlobal; }
    public void setGlobal(boolean global) { isGlobal = global; }
}
