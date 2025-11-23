package com.example.lotteryevent.data;

import com.google.firebase.Timestamp;
import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

/**
 * Unit tests for the Event data class.
 * These tests verify the constructors and the integrity of each property's getter and setter.
 */
public class EventTest {

    /**
     * Tests the public no-argument constructor required by Firestore.
     * Verifies that a new Event object is created with default values (null, 0, false).
     */
    @Test
    public void testNoArgumentConstructor() {
        Event event = new Event();
        assertNotNull("Event object should not be null", event);

        // Verify all object types are null
        assertNull("eventId should be null", event.getEventId());
        assertNull("name should be null", event.getName());
        assertNull("description should be null", event.getDescription());
        assertNull("organizerId should be null", event.getOrganizerId());
        assertNull("organizerName should be null", event.getOrganizerName());
        assertNull("location should be null", event.getLocation());
        assertNull("price should be null", event.getPrice());
        assertNull("posterImageUrl should be null", event.getPosterImageUrl());
        assertNull("eventStartDateTime should be null", event.getEventStartDateTime());
        assertNull("eventEndDateTime should be null", event.getEventEndDateTime());
        assertNull("registrationStartDateTime should be null", event.getRegistrationStartDateTime());
        assertNull("registrationEndDateTime should be null", event.getRegistrationEndDateTime());
        assertNull("capacity should be null", event.getCapacity());
        assertNull("lotteryGuidelines should be null", event.getLotteryGuidelines());
        assertNull("waitinglistlimit should be null", event.getWaitingListLimit());
        assertNull("status should be null", event.getStatus());
        assertNull("createdAt should be null", event.getCreatedAt());

        // Verify primitive types are their default values
        assertFalse("getGeolocationRequired should be false", event.getGeoLocationRequired());
    }

    /**
     * Tests the parameterized constructor for essential details.
     * Verifies that the specified fields are set correctly and others remain default.
     */
    @Test
    public void testParameterizedConstructor() {
        String name = "Community BBQ";
        String description = "A fun summer event.";
        String organizerId = "org-123";
        String organizerName = "Parks Dept.";
        Integer capacity = 150;

        Event event = new Event(name, description, organizerId, organizerName, capacity);

        assertNotNull("Event object should not be null", event);
        assertEquals("Name should be set by constructor", name, event.getName());
        assertEquals("Description should be set by constructor", description, event.getDescription());
        assertEquals("OrganizerId should be set by constructor", organizerId, event.getOrganizerId());
        assertEquals("OrganizerName should be set by constructor", organizerName, event.getOrganizerName());
        assertEquals("Capacity should be set by constructor", capacity, event.getCapacity());

        // Verify other fields are still default
        assertNull("eventId should be null", event.getEventId());
        assertNull("location should be null", event.getLocation());
    }

    /**
     * Tests the individual getters and setters for each property of the Event class.
     * Each method below targets a specific property to ensure it functions correctly.
     */
    @Test
    public void testGettersAndSetters() {
        Event event = new Event();

        // Test String properties
        String eventId = "evt-abcde";
        event.setEventId(eventId);
        assertEquals(eventId, event.getEventId());

        String name = "Tech Conference 2025";
        event.setName(name);
        assertEquals(name, event.getName());

        String description = "A conference about future tech.";
        event.setDescription(description);
        assertEquals(description, event.getDescription());

        String organizerId = "org-fghij";
        event.setOrganizerId(organizerId);
        assertEquals(organizerId, event.getOrganizerId());

        String organizerName = "Tech Corp";
        event.setOrganizerName(organizerName);
        assertEquals(organizerName, event.getOrganizerName());

        String location = "123 Main St, Anytown";
        event.setLocation(location);
        assertEquals(location, event.getLocation());

        String posterImageUrl = "http://example.com/poster.jpg";
        event.setPosterImageUrl(posterImageUrl);
        assertEquals(posterImageUrl, event.getPosterImageUrl());

        String lotteryGuidelines = "Winners will be selected randomly.";
        event.setLotteryGuidelines(lotteryGuidelines);
        assertEquals(lotteryGuidelines, event.getLotteryGuidelines());

        String status = "open";
        event.setStatus(status);
        assertEquals(status, event.getStatus());

        // Test Numeric properties
        Double price = 99.99;
        event.setPrice(price);
        assertEquals(price, event.getPrice());

        Integer capacity = 500;
        event.setCapacity(capacity);
        assertEquals(capacity, event.getCapacity());

        Integer waitinglistlimit = 50;
        event.setWaitingListLimit(waitinglistlimit);
        assertEquals(waitinglistlimit, event.getWaitingListLimit());

        // Test Boolean property
        event.setGeoLocationRequired(true);
        assertTrue(event.getGeoLocationRequired());

        // Test Timestamp properties
        Timestamp now = new Timestamp(new Date());
        event.setEventStartDateTime(now);
        assertEquals(now, event.getEventStartDateTime());

        event.setEventEndDateTime(now);
        assertEquals(now, event.getEventEndDateTime());

        event.setRegistrationStartDateTime(now);
        assertEquals(now, event.getRegistrationStartDateTime());

        event.setRegistrationEndDateTime(now);
        assertEquals(now, event.getRegistrationEndDateTime());

        event.setCreatedAt(now);
        assertEquals(now, event.getCreatedAt());
    }
}