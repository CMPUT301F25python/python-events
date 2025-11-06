package com.example.lotteryevent;

import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

public class CreateEventFragmentTest {

    private CreateEventFragment fragment;

    @Before
    public void setUp() {
        // Create a new instance of the fragment before each test
        fragment = new CreateEventFragment();
    }

    // Helper Timestamps for testing
    private Timestamp now = new Timestamp(new Date());
    private Timestamp oneHourFromNow = new Timestamp(new Date(System.currentTimeMillis() + 3600 * 1000));
    private Timestamp oneHourAgo = new Timestamp(new Date(System.currentTimeMillis() - 3600 * 1000));

    @Test
    public void testValidation_Success() {
        // Scenario: All inputs are valid
        String result = fragment.validateEventInput(
                "Valid Event",
                "2025-12-25", "10:00",
                "2025-12-25", "12:00",
                now, oneHourFromNow
        );
        // Assert that the result is null, meaning no error
        assertNull("Validation should pass with valid inputs", result);
    }

    @Test
    public void testValidation_EmptyEventName() {
        // Scenario: Event name is missing
        String result = fragment.validateEventInput(
                "", // Empty name
                "2025-12-25", "10:00",
                "2025-12-25", "12:00",
                now, oneHourFromNow
        );
        // Assert that the correct error message is returned
        assertEquals("Event name is required.", result);
    }

    @Test
    public void testValidation_MissingStartTime() {
        // Scenario: Start date is set, but start time is missing
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-12-25", "", // Start time is empty
                "2025-12-25", "12:00",
                null, oneHourFromNow // Start timestamp would be null
        );
        assertEquals("Please select a start time for the selected start date.", result);
    }

    @Test
    public void testValidation_MissingEndTime() {
        // Scenario: End date is set, but end time is missing
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-12-25", "10:00",
                "2025-12-25", "", // End time is empty
                now, null // End timestamp would be null
        );
        assertEquals("Please select an end time for the selected end date.", result);
    }

    @Test
    public void testValidation_StartTimeAfterEndTime() {
        // Scenario: Start time is after end time
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-12-25", "12:00",
                "2025-12-25", "10:00",
                oneHourFromNow, now // Start is after End
        );
        assertEquals("Event start time must be before event end time.", result);
    }

    @Test
    public void testValidation_StartTimeEqualsEndTime() {
        // Scenario: Start time is the same as end time
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-12-25", "10:00",
                "2025-12-25", "10:00",
                now, now // Timestamps are identical
        );
        assertEquals("Event start time must be before event end time.", result);
    }
}
