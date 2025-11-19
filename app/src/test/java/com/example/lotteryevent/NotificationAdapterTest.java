package com.example.lotteryevent;

import static org.junit.Assert.assertEquals;

import com.example.lotteryevent.adapters.NotificationAdapter;
import com.example.lotteryevent.data.Notification;

import org.junit.Before;
import org.junit.Test;


/**
 * Unit tests for the message composition logic within the {@link NotificationAdapter}.
 * <p>
 * This class tests the {@code messageConstructor} method to ensure messages constructed properly
 * based on message type for notifs.
 */
public class NotificationAdapterTest {

    private Notification notification;

    private String uid;
    private String title;
    private String message;
    private String type;
    private String eventId;
    private String eventName;
    private String organizerId;
    private String organizerName;

    /**
     * Sets up notif record and fields
     */
    @Before
    public void setUp() {
        uid = "Wa54uQ98ykOHpRq0IbZtMJoHAkU2";
        title = "Test Title";
        message = "Test Message";
        type = "lottery_win";
        eventId = "2HY5fqDbrNjxiTGjNh8J";
        eventName = "Event Name";
        organizerId = "aIJocfaDTrWhIp3N3FjvADBbTuv";
        organizerName = "Organizer Name";
        notification = new Notification(uid, title, message, type, eventId, eventName, organizerId, organizerName);
    }

    /**
     * Tests lottery win message construction
     */
    @Test
    public void testMessageConstructorLotteryWin() {
        notification.setType("lottery_win");
        String message = NotificationAdapter.messageConstructor(notification);
        assertEquals(message, "\uD83C\uDF89 You've been selected for " + notification.getEventName() + "! Tap to accept or decline.");
    }

    /**
     * Tests lottery loss message construction
     */
    @Test
    public void testMessageConstructorLotteryLoss() {
        notification.setType("lottery_loss");
        String message = NotificationAdapter.messageConstructor(notification);
        assertEquals(message, "❌ You weren't selected for " + notification.getEventName() + ".");
    }

    /**
     * Tests event update message construction
     */
    @Test
    public void testMessageConstructorEventUpdate() {
        notification.setType("event_update");
        String message = NotificationAdapter.messageConstructor(notification);
        assertEquals(message, "\uD83D\uDD01 A spot just opened for " + notification.getEventName() + "!");
    }

    /**
     * Tests custom message construction
     */
    @Test
    public void testMessageConstructorCustomMessage() {
        notification.setType("custom_message");
        String message = NotificationAdapter.messageConstructor(notification);
        assertEquals(message, "\uD83D\uDCAC Message from the organizer of " + notification.getEventName() + ": " + notification.getMessage());
    }

}
