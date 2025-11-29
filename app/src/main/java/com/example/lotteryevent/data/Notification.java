package com.example.lotteryevent.data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Represents a single notification in the lottery system.
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
    private Integer notifBannerId;

    /**
     * A public no-argument constructor is required for Firestore deserialization.
     */
    public Notification() {}

    /**
     * Constructs a new Notification with details
     * @param uid id of the recipient user
     * @param title title of the notif
     * @param message body of the notif
     * @param type type of notif
     * @param eventId event id that notif came from
     * @param eventName event name notif came from
     * @param organizerId organizer Id who sent the notif
     * @param organizerName organizer name who sent the notif
     */
    public Notification(String uid, String title, String message, String type, String eventId, String eventName, String organizerId, String organizerName, Integer notifBannerId) {
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
        this.notifBannerId = notifBannerId;
    }

    // --- Getters and Setters ---
    // Firestore uses these to populate the object's fields.

    /**
     * Gets the notification id
     * @return notif id
     */
    public String getNotificationId() {
        return notificationId;
    }

    /**
     * gets the event id
     * @return event id
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * gets the event name
     * @return event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * gets the message of the notif
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * gets the organizer ID
     * @return organizer ID
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * gets the organizer name
     * @return organizer name
     */
    public String getOrganizerName() {
        return organizerName;
    }

    /**
     * gets the recipient user's id
     * @return user id
     */
    public String getRecipientId() {
        return recipientId;
    }

    /**
     * gets boolean of whether notif has been seen
     * @return seen boolean
     */
    public Boolean getSeen() {
        return seen;
    }

    /**
     * gets title of notif
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * gets type of notif
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * gets timestamp of notif
     * @return timestamp
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * gets banner ID of notif
     * @return int of banner ID
     */
    public Integer getNotifBannerId() { return notifBannerId; }

    /**
     * gets notif id
     * @param notificationId notif id
     */
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    /**
     * sets the event id
     * @param eventId the event id
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * sets the event name
     * @param eventName the event name
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * sets the message
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * sets the organizer id
     * @param organizerId organizer id
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * sets the organizer name
     * @param organizerName organizer name
     */
    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    /**
     * sets the recipient user's id
     * @param recipientId user id
     */
    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    /**
     * sets whether the notif has been seen
     * @param seen boolean of whether it has been seen
     */
    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

    /**
     * sets the title
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * sets the notif type
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * sets the timestamp
     * @param timestamp the timestamp
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * sets the banner ID
     * @param notifBannerId ID to set
     */
    public void setNotifBannerId(Integer notifBannerId) { this.notifBannerId = notifBannerId; }
}
