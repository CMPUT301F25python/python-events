package com.example.lotteryevent.data;

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
     * @param eventId the event ID
     * @param eventName the event name
     * @param label the status assigned to the user based on draw results and displayed in UI
     * @param selected whether or not the user won the lottery
     */
    public RegistrationHistoryItem(String eventId, String eventName, String label, boolean selected) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.label = label;
        this.selected = selected;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getLabel() {
        return label;
    }

    public boolean isSelected() {
        return selected;
    }
}
