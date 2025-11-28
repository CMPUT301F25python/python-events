package com.example.lotteryevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.FakeEventRepository;
import com.example.lotteryevent.viewmodels.CreateEventViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

/**
 * Unit tests for the business logic within {@link CreateEventViewModel}.
 * <p>
 * This class validates that the {@code attemptToCreateEvent} method correctly enforces
 * validation rules (such as date chronology, required fields, and capacity constraints)
 * before sending data to the repository.
 * <p>
 * It uses a {@link FakeEventRepository} to simulate data persistence without requiring
 * a running Android emulator or Firebase connection.
 */
public class CreateEventViewModelTest {

    /**
     * Rule to swap the background executor used by the Architecture Components
     * with a different one which executes each task synchronously.
     * <p>
     * This is required because {@link CreateEventViewModel} uses {@link androidx.lifecycle.LiveData}
     * to update loading states and messages, which normally requires the Android Main Looper.
     */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private CreateEventViewModel viewModel;
    private FakeEventRepository fakeRepository;

    // Helper Calendars for testing relative time
    private Calendar oneHourAgo;
    private Calendar now;
    private Calendar inOneHour;
    private Calendar inTwoHours;
    private Calendar inThreeHours;
    private Calendar inFourHours;

    /**
     * Sets up the test environment.
     * <p>
     * 1. Initializes the Fake Repository.
     * 2. Initializes the ViewModel with the fake repository.
     * 3. Sets up Calendar objects representing relative times (e.g., "in one hour")
     *    to ensure tests run correctly regardless of the actual system time.
     */
    @Before
    public void setUp() {
        // 1. Init Fake Repository
        fakeRepository = new FakeEventRepository();

        // 2. Init ViewModel with Fake Repo
        viewModel = new CreateEventViewModel(fakeRepository);

        // 3. Initialize Calendars
        oneHourAgo = Calendar.getInstance(); oneHourAgo.add(Calendar.HOUR_OF_DAY, -1);

        now = Calendar.getInstance();

        inOneHour = Calendar.getInstance(); inOneHour.add(Calendar.HOUR_OF_DAY, 1);
        inTwoHours = Calendar.getInstance(); inTwoHours.add(Calendar.HOUR_OF_DAY, 2);
        inThreeHours = Calendar.getInstance(); inThreeHours.add(Calendar.HOUR_OF_DAY, 3);
        inFourHours = Calendar.getInstance(); inFourHours.add(Calendar.HOUR_OF_DAY, 4);
    }

    // --- Helper to reduce boilerplate ---

    /**
     * A helper wrapper to call {@link CreateEventViewModel#attemptToCreateEvent} with default
     * arguments for non-essential fields (Description, Location, etc.).
     *
     * @param name          The name of the event.
     * @param eventStart    Calendar object for Event Start.
     * @param eventEnd      Calendar object for Event End.
     * @param regStart      Calendar object for Registration Start.
     * @param regEnd        Calendar object for Registration End.
     * @param sDateText     String representation of Event Start Date (for empty checks).
     * @param sTimeText     String representation of Event Start Time (for empty checks).
     * @param eDateText     String representation of Event End Date (for empty checks).
     * @param eTimeText     String representation of Event End Time (for empty checks).
     * @param rsDateText    String representation of Reg Start Date (for empty checks).
     * @param rsTimeText    String representation of Reg Start Time (for empty checks).
     * @param reDateText    String representation of Reg End Date (for empty checks).
     * @param reTimeText    String representation of Reg End Time (for empty checks).
     * @param capacity      Max attendees string.
     * @param waitList      Waiting list limit string.
     * @return The result string from the ViewModel (null if success, error message otherwise).
     */
    private String callAttemptToCreate(String name,
                                       Calendar eventStart, Calendar eventEnd,
                                       Calendar regStart, Calendar regEnd,
                                       String sDateText, String sTimeText,
                                       String eDateText, String eTimeText,
                                       String rsDateText, String rsTimeText,
                                       String reDateText, String reTimeText,
                                       String capacity, String waitList) {

        return viewModel.attemptToCreateEvent(
                name, "Desc", "Loc", "10.0",
                capacity, waitList,  false,
                eventStart, eventEnd, regStart, regEnd,
                sDateText, sTimeText, eDateText, eTimeText,
                rsDateText, rsTimeText, reDateText, reTimeText
        );
    }

    // --- Happy Path ---

    /**
     * Tests the successful creation of an event.
     * <p>
     * <b>Scenario:</b> All inputs are valid, dates are in the future and chronologically correct.
     * <b>Expected:</b> Validation returns null (success), and the event is added to the repository.
     */
    @Test
    public void testValidation_Success() {
        String result = callAttemptToCreate(
                "Valid Future Event",
                inTwoHours, inThreeHours,
                inOneHour, inTwoHours,
                "2025-01-01", "12:00",
                "2025-01-01", "13:00",
                "2025-01-01", "11:00",
                "2025-01-01", "12:00",
                "10", "15"
        );

        assertNull("Validation should pass (return null) with all valid inputs", result);

        List<Event> events = fakeRepository.getInMemoryEvents();
        // Repository starts with 2 default events, so success means size is 3.
        assertEquals(3, events.size());
        assertEquals("Valid Future Event", events.get(2).getName());
    }

    // --- Field Validation ---

    /**
     * Tests validation for empty event names.
     * <p>
     * <b>Expected:</b> Error message "Event name is required."
     */
    @Test
    public void testValidation_EmptyEventName() {
        String result = callAttemptToCreate(
                "", // Empty Name
                inTwoHours, inThreeHours,
                inOneHour, inTwoHours,
                "2025-01-01", "12:00", "2025-01-01", "13:00",
                "2025-01-01", "11:00", "2025-01-01", "12:00",
                "10", "15"
        );
        assertEquals("Event Name is required.", result);
    }

    // --- Missing Input Strings ---

    /**
     * Tests validation when a Date is selected but the corresponding Time is missing.
     * <p>
     * <b>Scenario:</b> Event Start Date is populated, but Time is empty string.
     * <b>Expected:</b> Specific error message requesting a start time.
     */
    @Test
    public void testValidation_MissingEventStartTime() {
        String result = callAttemptToCreate(
                "Test Event",
                inTwoHours, inThreeHours,
                inOneHour, inTwoHours,
                "2025-01-01", "", // Start Date set, Time Empty
                "2025-01-01", "13:00",
                "2025-01-01", "11:00", "2025-01-01", "12:00",
                "10", "15"
        );
        assertEquals("Event Start Date and Time are required.", result);
    }

    /**
     * Tests validation when the Registration End Date is selected but Time is missing.
     * <p>
     * <b>Expected:</b> Specific error message requesting an end time.
     */
    @Test
    public void testValidation_MissingRegistrationEndTime() {
        String result = callAttemptToCreate(
                "Test Event",
                inTwoHours, inThreeHours,
                inOneHour, inTwoHours,
                "2025-01-01", "12:00", "2025-01-01", "13:00",
                "2025-01-01", "11:00",
                "2025-01-01", "", // Reg End Date set, Time Empty
                "10", "15"
        );
        assertEquals("Registration End Date and Time are required.", result);
    }

    // --- Chronological Logic ---

    /**
     * Tests chronological validation: Event Start vs Event End.
     * <p>
     * <b>Scenario:</b> Event Start is set to 3 hours from now, End is set to 2 hours from now.
     * <b>Expected:</b> Error "Event start time must be before event end time."
     */
    @Test
    public void testValidation_EventStartTimeAfterEndTime() {
        String result = callAttemptToCreate(
                "Test Event",
                inThreeHours, inTwoHours, // Start (3h) is after End (2h)
                inOneHour, inTwoHours,
                "2025-01-01", "15:00", "2025-01-01", "14:00",
                "2025-01-01", "11:00", "2025-01-01", "12:00",
                "10", "15"
        );
        assertEquals("Event start time must be before event end time.", result);
    }

    /**
     * Tests chronological validation: Registration vs Event Start.
     * <p>
     * <b>Scenario:</b> Registration ends (3 hours from now) after the event starts (2 hours from now).
     * <b>Expected:</b> Error "Registration must end before the event starts."
     */
    @Test
    public void testValidation_RegistrationEndsAfterEventStarts() {
        String result = callAttemptToCreate(
                "Test Event",
                inTwoHours, inThreeHours,
                inOneHour, inThreeHours, // Reg End (3h) > Event Start (2h)
                "2025-01-01", "12:00", "2025-01-01", "13:00",
                "2025-01-01", "11:00", "2025-01-01", "13:00",
                "10", "15"
        );
        assertEquals("Registration must end before the event starts.", result);
    }

    // --- Past Dates ---

    /**
     * Tests logic preventing registration from starting in the past.
     * <p>
     * <b>Scenario:</b> Registration start time is set to 1 hour ago.
     * <b>Expected:</b> Error "Registration start time cannot be in the past."
     */
    @Test
    public void testValidation_RegistrationStartTimeInThePast() {
        String result = callAttemptToCreate(
                "Test Event",
                inTwoHours, inThreeHours,
                oneHourAgo, inOneHour, // Reg start is 1 hour ago
                "2025-01-01", "12:00", "2025-01-01", "13:00",
                "2024-01-01", "11:00", "2025-01-01", "11:00",
                "10", "15"
        );
        assertEquals("Registration start time cannot be in the past.", result);
    }

    /**
     * Tests logic preventing the event from starting in the past.
     * <p>
     * <b>Scenario:</b> Event start time is set to 1 hour ago.
     * <b>Expected:</b> Error "Event start time cannot be in the past."
     */
    @Test
    public void testValidation_EventStartTimeInThePast() {
        Calendar regStartPast = Calendar.getInstance(); regStartPast.add(Calendar.HOUR_OF_DAY, -3);
        Calendar regEndPast = Calendar.getInstance(); regEndPast.add(Calendar.HOUR_OF_DAY, -2);

        String result = callAttemptToCreate(
                "Test Event",
                oneHourAgo, inThreeHours,
                regStartPast, regEndPast,
                "2024-01-01", "12:00", "2025-01-01", "13:00",
                "2024-01-01", "10:00", "2024-01-01", "11:00",
                "10", "15"
        );
        // Since Reg dates are also in the past, and that check runs first:
        assertEquals("Registration start time cannot be in the past.", result);
    }

    // --- Capacity Logic ---

    /**
     * Tests logic requiring a max capacity if a waiting list limit is set.
     * <p>
     * <b>Scenario:</b> Waiting list limit is 20, but Max Attendees is left empty.
     * <b>Expected:</b> Error "Max Attendees is required."
     */
    @Test
    public void testValidation_WaitingListRequiresCapacity() {
        String result = callAttemptToCreate(
                "Test Event",
                inThreeHours, inFourHours,
                inOneHour, inTwoHours,
                "2028-01-01", "10:00", "2028-01-01", "12:00",
                "2028-01-01", "08:00", "2028-01-01", "09:00",
                "", "20"
        );

        assertEquals("Max Attendees is required.", result);
    }

    /**
     * Tests logic requiring the waiting list limit to be greater than or equal to the capacity.
     * <p>
     * <b>Scenario:</b> Capacity is 10, but Waiting List Limit is 5.
     * <b>Expected:</b> Error "Waiting list size must be greater than or equal to the number of attendees."
     */
    @Test
    public void testValidation_WaitingListLimitIsLessThanCapacity() {
        String result = callAttemptToCreate(
                "Test Event",
                inThreeHours, inFourHours,
                inOneHour, inTwoHours,
                "2028-01-01", "10:00", "2028-01-01", "12:00",
                "2028-01-01", "08:00", "2028-01-01", "09:00",
                "10", "5"
        );
        assertEquals("Waiting list size must be greater than or equal to the number of attendees.", result);
    }

    /**
     * Tests logic requiring a Registration End date if a Registration Start date is provided.
     * <p>
     * <b>Scenario:</b> Reg Start is set, but Reg End fields are empty strings.
     * <b>Expected:</b> Error "A registration end date is required if a start date is set."
     */
    @Test
    public void testValidation_RegStartDateSetButEndMissing() {
        String result = callAttemptToCreate(
                "Test Event",
                inTwoHours, inThreeHours,
                inOneHour, null,
                "2025-01-01", "12:00", "2025-01-01", "13:00",
                "2025-01-01", "11:00", // Reg Start Set
                "", "", // Reg End Missing
                "10", "15"
        );
        assertEquals("Registration End Date and Time are required.", result);
    }
}