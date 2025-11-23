package com.example.lotteryevent.data;

/**
 * This class creates an item that will represent each row in the registration history fragment.
 * This will be displayed in the dynamic container.
 */
public class RegistrationHistoryItem {
    private String eventId;
    private String eventName;
    private String status; // (waiting = not selected, invited = selected)

    /**
     * A public no-argument constructor is required for Firestore deserialization.
     */
    public RegistrationHistoryItem(){
    }

    /**
     * Returns the unique identifier associated with the event.
     * @return the event ID as a String
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the eventId
     * @param eventId the event ID as a String
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the name associated with the event.
     * @return the name as a String
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the eventName
     * @param eventName the name as a String
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Returns the status assigned to the entrant based on draw results.
     * @return the assigned status as a String
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the draw
     * @param status the user's draw status as a String
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
