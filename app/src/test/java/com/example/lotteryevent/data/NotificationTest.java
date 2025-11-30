package com.example.lotteryevent.data;

import com.google.firebase.Timestamp;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link Notification} data model.
 * <p>
 * This class verifies the behavior of the Notification POJO, including:
 * <ul>
 *     <li>Correct initialization via constructors.</li>
 *     <li>Proper functionality of getter and setter methods.</li>
 *     <li>Default values for fields like 'seen' and 'timestamp'.</li>
 * </ul>
 */
public class NotificationTest {

    /**
     * Verifies that the no-argument constructor creates an instance
     * and that fields default to null (or specific defaults) as expected.
     */
    @Test
    public void testNoArgumentConstructor() {
        Notification notification = new Notification();

        assertNotNull("Notification instance should be created", notification);
        assertNull("NotificationId should be null initially", notification.getNotificationId());
        assertNull("Title should be null initially", notification.getTitle());
        assertNull("Seen should be null initially (for Firestore mapping)", notification.getSeen());
    }

    /**
     * Verifies that the parameterized constructor correctly initializes all fields
     * passed to it, and sets default values for fields like 'seen' and 'timestamp'.
     */
    @Test
    public void testParameterizedConstructor() {
        String uid = "user123";
        String title = "Test Title";
        String message = "Test Message";
        String type = "lottery_win";
        String eventId = "event123";
        String eventName = "Gala";
        String senderId = "org123";
        String senderName = "Organizer Name";
        Integer notifBannerId = 123;

        Notification notification = new Notification(uid, title, message, type, eventId, eventName, senderId, senderName, notifBannerId);

        assertEquals("Recipient ID should match", uid, notification.getRecipientId());
        assertEquals("Title should match", title, notification.getTitle());
        assertEquals("Message should match", message, notification.getMessage());
        assertEquals("Type should match", type, notification.getType());
        assertEquals("Event ID should match", eventId, notification.getEventId());
        assertEquals("Event Name should match", eventName, notification.getEventName());
        assertEquals("Sender ID should match", senderId, notification.getSenderId());
        assertEquals("Sender Name should match", senderName, notification.getSenderName());
        assertEquals("Notifications Banner ID should match", notifBannerId, notification.getNotifBannerId());

        // Verify defaults set in constructor
        assertFalse("New notification should default to unseen", notification.getSeen());
        assertNotNull("Timestamp should be generated in constructor", notification.getTimestamp());
    }

    /**
     * Verifies that the {@code setNotificationId} and {@code getNotificationId} methods
     * correctly store and retrieve the document ID.
     */
    @Test
    public void testSetAndGetNotificationId() {
        Notification notification = new Notification();
        String id = "doc_id_123";

        notification.setNotificationId(id);

        assertEquals("Notification ID should match set value", id, notification.getNotificationId());
    }

    /**
     * Verifies that the {@code setSeen} and {@code getSeen} methods correctly update
     * the read status of the notification.
     */
    @Test
    public void testSetAndGetSeenStatus() {
        Notification notification = new Notification();

        // Test setting to true
        notification.setSeen(true);
        assertTrue("Seen status should be true", notification.getSeen());

        // Test setting back to false
        notification.setSeen(false);
        assertFalse("Seen status should be false", notification.getSeen());
    }

    /**
     * Verifies that the {@code setTimestamp} and {@code getTimestamp} methods
     * correctly handle Firebase Timestamp objects.
     */
    @Test
    public void testSetAndGetTimestamp() {
        Notification notification = new Notification();
        Timestamp now = Timestamp.now();

        notification.setTimestamp(now);

        assertEquals("Timestamp should match set value", now, notification.getTimestamp());
    }

    /**
     * Verifies various event-related getters and setters (EventId, EventName, Type).
     */
    @Test
    public void testEventDetailsGettersAndSetters() {
        Notification notification = new Notification();
        String eventId = "ev_1";
        String eventName = "Party";
        String type = "general";
        Integer notifBannerId = 123;

        notification.setEventId(eventId);
        notification.setEventName(eventName);
        notification.setType(type);
        notification.setNotifBannerId(notifBannerId);

        assertEquals(eventId, notification.getEventId());
        assertEquals(eventName, notification.getEventName());
        assertEquals(type, notification.getType());
        assertEquals(notifBannerId, notification.getNotifBannerId());
    }

    /**
     * Verifies various sender-related getters and setters.
     */
    @Test
    public void testSenderDetailsGettersAndSetters() {
        Notification notification = new Notification();
        String orgId = "org_1";
        String orgName = "Committee";

        notification.setSenderId(orgId);
        notification.setSenderName(orgName);

        assertEquals(orgId, notification.getSenderId());
        assertEquals(orgName, notification.getSenderName());
    }

    /**
     * Verifies the recipient-related getters and setters.
     */
    @Test
    public void testRecipientGettersAndSetters() {
        Notification notification = new Notification();
        String recipientId = "user_99";

        notification.setRecipientId(recipientId);

        assertEquals(recipientId, notification.getRecipientId());
    }

    /**
     * Verifies the core content (Title, Message) getters and setters.
     */
    @Test
    public void testContentGettersAndSetters() {
        Notification notification = new Notification();
        String title = "Update";
        String message = "Details changed";

        notification.setTitle(title);
        notification.setMessage(message);

        assertEquals(title, notification.getTitle());
        assertEquals(message, notification.getMessage());
    }
}