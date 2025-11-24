package com.example.lotteryevent.data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Represents a single event in the lottery system.
 * This class is a Plain Old Java Object (POJO) used for mapping Firestore documents.
 */
public class Event {

    // By annotating this field with @DocumentId, Firestore will automatically populate it
    // with the document's ID when you read data.
    @DocumentId
    private String eventId;

    private String name;
    private String description;
    private String organizerId;
    private String organizerName;
    private String location;
    private Double price;
    private String posterImageUrl;
    private String posterBase64;
    private Timestamp eventStartDateTime;
    private Timestamp eventEndDateTime;
    private Timestamp registrationStartDateTime;
    private Timestamp registrationEndDateTime;
    private Integer capacity;
    private Integer waitingListLimit;
    private boolean geoLocationRequired;
    private String status;

    // @ServerTimestamp tells Firestore to automatically populate this field with the
    // server's timestamp when the document is first created. It will be null until then.
    @ServerTimestamp
    private Timestamp createdAt;


    /**
     * A public no-argument constructor is required for Firestore deserialization.
     */
    public Event() {}

    /**
     * Constructs a new Event with essential details.
     * @param name The name of the event.
     * @param description A short description of the event.
     * @param organizerId The ID of the user who organized the event.
     * @param organizerName The name of the user who organized the event.
     * @param capacity The maximum number of attendees for the event.
     */
    public Event(String name, String description, String organizerId, String organizerName, Integer capacity) {
        this.name = name;
        this.description = description;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.capacity = capacity;
    }

    // --- Getters and Setters ---
    // Firestore uses these to populate the object's fields.

    /**
     * Gets the unique ID of the event.
     * @return The event ID.
     */
    public String getEventId() { return eventId; }
    /**
     * Sets the unique ID of the event.
     * @param eventId The event ID.
     */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /**
     * Gets the name of the event.
     * @return The event name.
     */
    public String getName() { return name; }
    /**
     * Sets the name of the event.
     * @param name The event name.
     */
    public void setName(String name) { this.name = name; }

    /**
     * Gets the description of the event.
     * @return The event description.
     */
    public String getDescription() { return description; }
    /**
     * Sets the description of the event.
     * @param description The event description.
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Gets the ID of the event organizer.
     * @return The organizer's user ID.
     */
    public String getOrganizerId() { return organizerId; }
    /**
     * Sets the ID of the event organizer.
     * @param organizerId The organizer's user ID.
     */
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    /**
     * Gets the name of the event organizer.
     * @return The organizer's name.
     */
    public String getOrganizerName() { return organizerName; }
    /**
     * Sets the name of the event organizer.
     * @param organizerName The organizer's name.
     */
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    /**
     * Gets the location of the event.
     * @return The event location.
     */
    public String getLocation() { return location; }
    /**
     * Sets the location of the event.
     * @param location The event location.
     */
    public void setLocation(String location) { this.location = location; }

    /**
     * Gets the price of the event.
     * @return The event price.
     */
    public Double getPrice() { return price; }
    /**
     * Sets the price of the event.
     * @param price The event price.
     */
    public void setPrice(Double price) { this.price = price; }

    /**
     * Gets the URL for the event poster image.
     * @return The poster image URL.
     */
    public String getPosterImageUrl() { return posterImageUrl; }
    /**
     * Sets the URL for the event poster image.
     * @param posterImageUrl The poster image URL.
     */
    public void setPosterImageUrl(String posterImageUrl) { this.posterImageUrl = posterImageUrl; }

    /**
     * Gets the start date and time of the event.
     * @return The event start timestamp.
     */
    public Timestamp getEventStartDateTime() { return eventStartDateTime; }
    /**
     * Sets the start date and time of the event.
     * @param eventStartDateTime The event start timestamp.
     */
    public void setEventStartDateTime(Timestamp eventStartDateTime) { this.eventStartDateTime = eventStartDateTime; }

    /**
     * Gets the end date and time of the event.
     * @return The event end timestamp.
     */
    public Timestamp getEventEndDateTime() { return eventEndDateTime; }
    /**
     * Sets the end date and time of the event.
     * @param eventEndDateTime The event end timestamp.
     */
    public void setEventEndDateTime(Timestamp eventEndDateTime) { this.eventEndDateTime = eventEndDateTime; }

    /**
     * Gets the start date and time for registration.
     * @return The registration start timestamp.
     */
    public Timestamp getRegistrationStartDateTime() { return registrationStartDateTime; }
    /**
     * Sets the start date and time for registration.
     * @param registrationStartDateTime The registration start timestamp.
     */
    public void setRegistrationStartDateTime(Timestamp registrationStartDateTime) { this.registrationStartDateTime = registrationStartDateTime; }



    public Timestamp getRegistrationEndDateTime() { return registrationEndDateTime; }
    public void setRegistrationEndDateTime(Timestamp registrationEndDateTime) { this.registrationEndDateTime = registrationEndDateTime; }

    /**
     * Gets the maximum capacity of the event.
     * @return The event capacity.
     */
    public Integer getCapacity() { return capacity; }
    /**
     * Sets the maximum capacity of the event.
     * @param capacity The event capacity.
     */
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    /**
     * Gets the waiting list limit for the event.
     * @return The waiting list limit.
     */
    public Integer getWaitingListLimit() { return waitingListLimit; }
    /**
     * Sets the waiting list limit for the event.
     * @param waitingListLimit The waiting list limit.
     */
    public void setWaitingListLimit(Integer waitingListLimit) { this.waitingListLimit = waitingListLimit; }

    /**
     * Checks if geolocation is required for the event.
     * @return True if geolocation is required, false otherwise.
     */
    public boolean getGeoLocationRequired() { return geoLocationRequired; }
    /**
     * Sets whether geolocation is required for the event.
     * @param geoLocationRequired True if geolocation is required, false otherwise.
     */
    public void setGeoLocationRequired(boolean geoLocationRequired) { this.geoLocationRequired = geoLocationRequired; }

    /**
     * Gets the status of the event.
     * @return The event status.
     */
    public String getStatus() { return status; }
    /**
     * Sets the status of the event.
     * @param status The event status.
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * Gets the timestamp when the event was created.
     * @return The creation timestamp.
     */
    public Timestamp getCreatedAt() { return createdAt; }
    /**
     * Sets the timestamp when the event was created.
     * @param createdAt The creation timestamp.
     */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    /**
     * Gets the organizer-defined guidelines for the lottery selection process.
     * @return The lottery guidelines.
     */
    public String getLotteryGuidelines() {
        return lotteryGuidelines;
    }

    /**
     * Sets the organizer-defined guidelines for the lottery selection process.
     * @param lotteryGuidelines The lottery guidelines.
     */
    public void setLotteryGuidelines(String lotteryGuidelines) {
        this.lotteryGuidelines = lotteryGuidelines;
    }

    /**
     * Gets the Base64-encoded contents of the event poster image.
     * @return The Base64 poster image data, or {@code null} if none is set.
     */
    public String getPosterBase64() { return posterBase64; }

    /**
     * Sets the Base64-encoded contents of the event poster image.
     * @param posterBase64 The Base64 poster image data.
     */
    public void setPosterBase64(String posterBase64) { this.posterBase64 = posterBase64; }

}
