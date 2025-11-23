package com.example.lotteryevent.data;

/**
 * This class creates item that will represent each row in the registration history fragment.
 * This will be displayed in the dynamic container.
 */
public class RegistrationHistoryItem {
    private String eventId;
    private String eventName;
    private String label; // (selected, not selected, waiting)
    private boolean selected; // flag (true if selected)

    /**
     * A public no-argument constructor is required for Firestore deserialization.
     */
    private RegistrationHistoryItem(){
    }

    /**
     * This constructor initializes the instances of the four parameters
     * @param eventId the event ID as a String
     * @param eventName the event name as a String
     * @param label the status assigned to the user based on draw results and displayed in UI as a String
     * @param selected whether or not the user won the lottery as a Boolean
     */
    public RegistrationHistoryItem(String eventId, String eventName, String label, boolean selected) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.label = label;
        this.selected = selected;
    }

    /**
     * Returns the unique identifier associated with the event.
     * @return the event ID as a String
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns the name associated with the event.
     * @return the name as a String
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Returns the label assigned to the entrant based on draw results.
     * @return the assigned label as a String
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns whether or not an entrant was selected
     * @return value of selected as a Boolean
     */
    public boolean isSelected() {
        return selected;
    }
}
