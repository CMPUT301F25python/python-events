package com.example.lotteryevent.data;

/**
 * A simple data model representing an image associated with an event for administrative purposes.
 * <p>
 * This class holds the minimal information required to display and manage an image
 * in the admin dashboard: the unique ID of the event it belongs to and the
 * Base64 string representation of the image itself.
 * </p>
 */
public class AdminImageItem {
    private String eventId;
    private String base64Image;

    /**
     * Constructs a new AdminImageItem.
     *
     * @param eventId     The unique identifier of the event associated with this image.
     * @param base64Image The Base64 encoded string representation of the image.
     */
    public AdminImageItem(String eventId, String base64Image) {
        this.eventId = eventId;
        this.base64Image = base64Image;
    }

    /**
     * Retrieves the unique event ID associated with this image.
     *
     * @return The event ID string.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Retrieves the Base64 encoded string of the image.
     *
     * @return A Base64 string representing the image bitmap.
     */
    public String getBase64Image() {
        return base64Image;
    }
}
