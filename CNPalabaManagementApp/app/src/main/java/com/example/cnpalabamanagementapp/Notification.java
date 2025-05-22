package com.example.cnpalabamanagementapp;

import java.util.Date;

public class Notification {
    private String id;
    private NotificationType type;
    private String subject;
    private String content;
    private Date timestamp;
    private boolean isRead;
    private String announcementId;
    private String fromUserId;
    private String fromUsername;
    private String toUsername;

    public enum NotificationType {
        MESSAGE, REPORT, ANNOUNCEMENT
    }

    // Empty constructor needed for Firestore
    public Notification() {}

    // Constructor
    public Notification(String id, NotificationType type, String subject, String content,
                        Date timestamp, boolean isRead, String userId, String announcementId,
                        String fromUserId, String fromUsername, String toUsername) {
        this.id = id;
        this.type = type;
        this.subject = subject;
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.announcementId = announcementId;
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.toUsername = toUsername;
    }

    // Getters and Setters
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }  // Add this setter

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(String announcementId) { this.announcementId = announcementId; }
}