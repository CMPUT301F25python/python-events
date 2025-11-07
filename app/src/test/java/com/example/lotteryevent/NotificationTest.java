package com.example.lotteryevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.example.lotteryevent.data.Notification;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

/**
 * Unit Tests for Data Notification, testing its constructor, getters, and setters
 */
public class NotificationTest {

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
     * assigns fields for notif
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
    }

    /**
     * tests notif constructor
     */
    @Test
    public void testConstructor() {

        Notification notification = new Notification(uid, title, message, type, eventId, eventName, organizerId, organizerName);

        assertEquals(uid, notification.getRecipientId());
        assertEquals(title, notification.getTitle());
        assertEquals(message, notification.getMessage());
        assertEquals(type, notification.getType());
        assertEquals(eventId, notification.getEventId());
        assertEquals(eventName, notification.getEventName());
        assertEquals(organizerId, notification.getOrganizerId());
        assertEquals(organizerName, notification.getOrganizerName());
        assertEquals(false, notification.getSeen());
        assertNotEquals(null, notification.getTimestamp());
    }

    /**
     * tests notif's getters and setters
     */
    @Test
    public void testGettersSetters() {
        Notification notification = new Notification();
        Timestamp now = Timestamp.now();

        notification.setRecipientId(uid);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setEventId(eventId);
        notification.setEventName(eventName);
        notification.setOrganizerId(organizerId);
        notification.setOrganizerName(organizerName);
        notification.setSeen(true);
        notification.setTimestamp(now);
        notification.setNotificationId("12345abcde");

        assertEquals(uid, notification.getRecipientId());
        assertEquals(title, notification.getTitle());
        assertEquals(message, notification.getMessage());
        assertEquals(type, notification.getType());
        assertEquals(eventId, notification.getEventId());
        assertEquals(eventName, notification.getEventName());
        assertEquals(organizerId, notification.getOrganizerId());
        assertEquals(organizerName, notification.getOrganizerName());
        assertEquals(true, notification.getSeen());
        assertEquals(now, notification.getTimestamp());
        assertEquals("12345abcde", notification.getNotificationId());
    }
}
