package com.example.classmate;

public class Summary {
    private String id;
    private String title;
    private String course;
    private String imageUrl;      // URL Firebase Storage
    private String uploaderEmail;
    private String className;
    private long timestamp;

    public Summary() {}

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

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getCourse() { return course != null ? course : ""; }
    public void setCourse(String course) { this.course = course; }

    public String getImageUrl() { return imageUrl != null ? imageUrl : ""; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // Kept for backward compatibility
    public String getFilePath() { return getImageUrl(); }

    public String getUploaderEmail() { return uploaderEmail != null ? uploaderEmail : ""; }
    public void setUploaderEmail(String uploaderEmail) { this.uploaderEmail = uploaderEmail; }

    public String getClassName() { return className != null ? className : ""; }
    public void setClassName(String className) { this.className = className; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
