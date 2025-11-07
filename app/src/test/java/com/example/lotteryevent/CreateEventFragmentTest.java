package com.example.lotteryevent;

import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

/**
 * Unit tests for the validation logic within the {@link CreateEventFragment}.
 * <p>
 * This class tests the {@code validateEventInput} method to ensure all business rules
 * for creating an event are correctly enforced. It covers scenarios such as empty fields,
 * chronological order of dates, and the integrity of the registration period.
 */
public class CreateEventFragmentTest {

    private CreateEventFragment fragment;

    // Helper Timestamps for testing
    private Timestamp oneHourAgo;
    private Timestamp now;
    private Timestamp inOneHour;
    private Timestamp inTwoHours;
    private Timestamp inThreeHours;
    private Timestamp inFourHours;


    @Before
    public void setUp() {
        // Create a new instance of the fragment before each test
        fragment = new CreateEventFragment();

        // Initialize timestamps here to get fresh values for each test run
        long currentTime = System.currentTimeMillis();
        oneHourAgo = new Timestamp(new Date(currentTime - 3600 * 1000));
        now = new Timestamp(new Date(currentTime));
        inOneHour = new Timestamp(new Date(currentTime + 3600 * 1000));
        inTwoHours = new Timestamp(new Date(currentTime + 7200 * 1000));
        inThreeHours = new Timestamp(new Date(currentTime + 10800 * 1000));
        inFourHours = new Timestamp(new Date(currentTime + 14400 * 1000));
    }

    // All Valid Scenarios
    @Test
    public void testValidation_Success() {
        // Scenario: All inputs are valid and in the future
        String result = fragment.validateEventInput(
                "Valid Future Event",
                "2025-01-01", "10:00", // Event Start
                "2025-01-01", "12:00", // Event End
                inTwoHours, inThreeHours,      // Event Timestamps
                "2025-01-01", "08:00", // Reg Start
                "2025-01-01", "09:00", // Reg End
                inOneHour, inTwoHours,                // Reg Timestamps
                10, 15 // capacity and waiting list limit
        );
        assertNull("Validation should pass with all valid inputs", result);
    }

    // Basic Field Validation
    @Test
    public void testValidation_EmptyEventName() {
        String result = fragment.validateEventInput(
                "", // Empty name
                "2025-01-01", "10:00",
                "2025-01-01", "12:00",
                inTwoHours, inThreeHours,
                "2025-01-01", "08:00",
                "2025-01-01", "09:00",
                now, inOneHour,
                0, 0

        );
        assertEquals("Event name is required.", result);
    }

    // Incomplete Date/Time Pairs
    @Test
    public void testValidation_MissingEventStartTime() {
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-01-01", "", // Event Start Time is empty
                "2025-01-01", "12:00",
                null, inThreeHours,
                "2025-01-01", "08:00",
                "2025-01-01", "09:00",
                now, inOneHour,
                0, 0

        );
        assertEquals("Please select a start time for the selected event start date.", result);
    }

    @Test
    public void testValidation_MissingRegistrationEndTime() {
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-01-01", "10:00",
                "2025-01-01", "12:00",
                inTwoHours, inThreeHours,
                "2025-01-01", "08:00",
                "2025-01-01", "", // Reg End Time is empty
                now, null,
                0, 0
        );
        assertEquals("Please select an end time for the selected registration end date.", result);
    }


    // Start dates before end dates
    @Test
    public void testValidation_EventStartTimeAfterEndTime() {
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-01-01", "12:00",
                "2025-01-01", "10:00",
                inThreeHours, inTwoHours, // Start is after End
                "2025-01-01", "08:00",
                "2025-01-01", "09:00",
                now, inOneHour,
                0, 0
        );
        assertEquals("Event start time must be before event end time.", result);
    }

    @Test
    public void testValidation_RegistrationStartTimeEqualsEndTime() {
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-01-01", "12:00",
                "2025-01-01", "13:00",
                inTwoHours, inThreeHours,
                "2025-01-01", "10:00",
                "2025-01-01", "10:00",
                now, now, // Reg timestamps are identical
                0, 0
        );
        assertEquals("Registration start time must be before registration end time.", result);
    }

    @Test
    public void testValidation_RegistrationEndsAfterEventStarts() {
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-01-01", "10:00", // Event starts at 10
                "2025-01-01", "12:00",
                inTwoHours, inFourHours,
                "2025-01-01", "09:00",
                "2025-01-01", "11:00", // Registration ends at 11
                inOneHour, inThreeHours,
                0, 0
        );
        assertEquals("Registration must end before the event starts.", result);
    }


    // Past Date/Time Validation
    @Test
    public void testValidation_RegistrationStartTimeInThePast() {
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-01-01", "10:00",
                "2025-01-01", "12:00",
                inTwoHours, inThreeHours,
                "2024-01-01", "08:00", // Reg started in the past
                "2025-01-01", "09:00",
                oneHourAgo, inOneHour,
                0,0
        );
        assertEquals("Registration start time cannot be in the past.", result);
    }

    @Test
    public void testValidation_EventStartTimeInThePast() {
        String result = fragment.validateEventInput(
                "Test Event",
                "2024-01-01", "10:00", // Event started in the past
                "2025-01-01", "12:00",
                oneHourAgo, inThreeHours,  // The event start timestamp is in the past
                "", "",                  // <-- No registration start date
                "", "",                  // <-- No registration end date
                null, null,               // <-- Registration timestamps are null
                0, 0
        );
        assertEquals("Event start time cannot be in the past.", result);
    }

    // Validate registration period
    @Test
    public void testValidation_RegistrationStartDateSet_ButEndDateIsMissing() {
        // Scenario: A user sets a registration start date, but forgets the end date.
        String result = fragment.validateEventInput(
                "Test Event",
                "2025-01-01", "12:00",
                "2025-01-01", "13:00",
                inTwoHours, inThreeHours,
                "2025-01-01", "10:00", // Registration Start is set
                "", "",                  // Registration End is MISSING
                now, null,                // Timestamps reflect missing end date
                0,0
        );
        assertEquals("A registration end date is required if a start date is set.", result);
    }

    // Validate waiting list capacity size vs attendee size
    @Test
    public void testValidation_WaitingListLimitIsLessThanCapacity() {
        String result = fragment.validateEventInput("Valid Future Event",
                "2028-01-01", "10:00",
                "2028-01-01", "12:00",
                inThreeHours, inFourHours,
                "2028-01-01", "08:00",
                "2028-01-01", "09:00",
                inOneHour, inTwoHours,
                10, 5
        );
        assertEquals("Waiting list size must be greater than or equal to the number of attendees.", result);
    }

}
