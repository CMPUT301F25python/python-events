package com.example.lotteryevent.data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Represents a single event in the lottery system.
 * This class is a Plain Old Java Object (POJO) used for mapping Firestore documents.
 */
public class Notification {

    // By annotating this field with @DocumentId, Firestore will automatically populate it
    // with the document's ID when you read data.
    @DocumentId
    private String notificationId;

    private String eventId;
    private String eventName;
    private String message;
    private String organizerId;
    private String organizerName;
    private String recipientId;
    private Boolean seen;
    private String title;
    private String type;
    private Timestamp timestamp;

    /**
     * A public no-argument constructor is required for Firestore deserialization.
     */
    public Notification() {}

    /**
     * Constructs a new Event with essential details.
     */
    public Notification(String uid, String title, String message, String type, String eventId, String eventName, String organizerId, String organizerName) {
        this.recipientId = uid;
        this.title = title;
        this.message = message;
        this.type = type;
        this.eventId = eventId;
        this.eventName = eventName;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.seen = false;
        this.timestamp = Timestamp.now();
    }

    // --- Getters and Setters ---
    // Firestore uses these to populate the object's fields.

    public String getNotificationId() {
        return notificationId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getMessage() {
        return message;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public Boolean getSeen() {
        return seen;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
