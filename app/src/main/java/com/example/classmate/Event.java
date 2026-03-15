package com.example.classmate;

public class Event {
    private String id;
    private String subject;
    private String type;
    private long timestamp;
    private String material;
    private String notes;
    private String className;

    public Event() {}

    public Event(String id, String subject, String type, long timestamp,
                 String material, String notes, String className) {
        this.id = id;
        this.subject = subject;
        this.type = type;
        this.timestamp = timestamp;
        this.material = material;
        this.notes = notes;
        this.className = className;
    }

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
}
