package com.example.lotteryevent.data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;

/**
 * Represents a single entrant in an event's subcollection.
 * This POJO is a direct mapping of the '/events/{eventId}/entrants/{userId}' schema.
 */
public class Entrant {

    /**
     * The document ID in the 'entrants' subcollection is the user's ID.
     * The @DocumentId annotation tells Firestore to automatically populate this field.
     */
    @DocumentId
    private String userId;

    private String userName;
    private Timestamp dateRegistered;
    private GeoPoint geoLocation; // Firestore's GeoPoint is the correct type for this
    private String status;

    /**
     * A public no-argument constructor is required for Firestore deserialization.
     */
    public Entrant() {}

    // --- Getters and Setters ---
    // Firestore uses these to map data to and from the object.

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Timestamp getDateRegistered() {
        return dateRegistered;
    }

    public void setDateRegistered(Timestamp dateRegistered) {
        this.dateRegistered = dateRegistered;
    }

    public GeoPoint getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoPoint geoLocation) {
        this.geoLocation = geoLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}