package com.example.lotteryevent.data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for the Entrant data class.
 * These tests verify the integrity of the getters and setters and the default constructor.
 */
public class EntrantTest {

    /**
     * Tests the public no-argument constructor.
     * Verifies that a new Entrant object is instantiated with null fields,
     * which is required for Firestore deserialization.
     */
    @Test
    public void testNoArgumentConstructor() {
        Entrant entrant = new Entrant();
        assertNotNull("Entrant object should not be null", entrant);

        // Verify that all fields are initially null
        assertNull("userId should be null initially", entrant.getUserId());
        assertNull("userName should be null initially", entrant.getUserName());
        assertNull("dateRegistered should be null initially", entrant.getDateRegistered());
        assertNull("geoLocation should be null initially", entrant.getGeoLocation());
        assertNull("status should be null initially", entrant.getStatus());
    }

    /**
     * Tests the getter and setter for the 'userId' property.
     */
    @Test
    public void testGetAndSetUserId() {
        Entrant entrant = new Entrant();
        String expectedUserId = "user-12345";
        entrant.setUserId(expectedUserId);
        assertEquals("The userId should match the value that was set", expectedUserId, entrant.getUserId());
    }

    /**
     * Tests the getter and setter for the 'userName' property.
     */
    @Test
    public void testGetAndSetUserName() {
        Entrant entrant = new Entrant();
        String expectedUserName = "John Doe";
        entrant.setUserName(expectedUserName);
        assertEquals("The userName should match the value that was set", expectedUserName, entrant.getUserName());
    }

    /**
     * Tests the getter and setter for the 'dateRegistered' property.
     */
    @Test
    public void testGetAndSetDateRegistered() {
        Entrant entrant = new Entrant();
        Timestamp expectedTimestamp = new Timestamp(new Date()); // Use current time for the test
        entrant.setDateRegistered(expectedTimestamp);
        assertEquals("The dateRegistered should match the value that was set", expectedTimestamp, entrant.getDateRegistered());
    }

    /**
     * Tests the getter and setter for the 'geoLocation' property.
     */
    @Test
    public void testGetAndSetGeoLocation() {
        Entrant entrant = new Entrant();
        GeoPoint expectedGeoPoint = new GeoPoint(40.7128, -74.0060); // Example coordinates (New York City)
        entrant.setGeoLocation(expectedGeoPoint);
        assertEquals("The geoLocation should match the value that was set", expectedGeoPoint, entrant.getGeoLocation());
    }

    /**
     * Tests the getter and setter for the 'status' property.
     */
    @Test
    public void testGetAndSetStatus() {
        Entrant entrant = new Entrant();
        String expectedStatus = "accepted";
        entrant.setStatus(expectedStatus);
        assertEquals("The status should match the value that was set", expectedStatus, entrant.getStatus());
    }

    /**
     * A comprehensive test that sets all properties at once and then verifies them.
     * This ensures that setting one property does not unintentionally affect another.
     */
    @Test
    public void testAllProperties() {
        Entrant entrant = new Entrant();

        // --- Arrange: Define test data ---
        String expectedUserId = "user-all-props";
        String expectedUserName = "Jane Smith";
        Timestamp expectedTimestamp = Timestamp.now();
        GeoPoint expectedGeoPoint = new GeoPoint(34.0522, -118.2437); // Los Angeles
        String expectedStatus = "waiting";

        // --- Act: Set all properties ---
        entrant.setUserId(expectedUserId);
        entrant.setUserName(expectedUserName);
        entrant.setDateRegistered(expectedTimestamp);
        entrant.setGeoLocation(expectedGeoPoint);
        entrant.setStatus(expectedStatus);

        // --- Assert: Verify all properties ---
        assertEquals(expectedUserId, entrant.getUserId());
        assertEquals(expectedUserName, entrant.getUserName());
        assertEquals(expectedTimestamp, entrant.getDateRegistered());
        assertEquals(expectedGeoPoint, entrant.getGeoLocation());
        assertEquals(expectedStatus, entrant.getStatus());
    }
}