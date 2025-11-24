package com.example.lotteryevent.data;

public class AdminImageItem {
    private String eventId;
    private String base64Image;

    public AdminImageItem(String eventId, String base64Image) {
        this.eventId = eventId;
        this.base64Image = base64Image;
    }

    public String getEventId() { return eventId; }
    public String getBase64Image() { return base64Image; }
}
