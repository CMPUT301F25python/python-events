package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IEventRepository;
import com.google.firebase.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * ViewModel for the CreateEventFragment.
 * This class is responsible for handling the business logic of event creation,
 * including input validation and communication with the data layer (IEventRepository).
 */
public class CreateEventViewModel extends ViewModel {

    private final IEventRepository eventRepository;

    /**
     * Constructs the ViewModel with a dependency on the event repository.
     *
     * @param eventRepository An implementation of IEventRepository.
     */
    public CreateEventViewModel(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // --- LiveData Exposure ---
    // The ViewModel exposes the state from the repository directly to the UI.

    public LiveData<Boolean> isLoading() {
        return eventRepository.isLoading();
    }

    public LiveData<String> getMessage() {
        return eventRepository.getMessage();
    }

    /**
     * The main entry point for the Fragment. It orchestrates the validation and creation of an event.
     *
     * @return An error message string if validation fails, otherwise null.
     */
    public String attemptToCreateEvent(String eventName, String description, String location,
                                       String priceStr, String maxAttendeesStr, String waitingListLimitStr,
                                       String lotteryGuidelinesStr, boolean isGeoLocationRequired,
                                       Calendar eventStartCalendar, Calendar eventEndCalendar,
                                       Calendar registrationStartCalendar, Calendar registrationEndCalendar,
                                       String startDateText, String startTimeText, String endDateText, String endTimeText,
                                       String regStartDateText, String regStartTimeText, String regEndDateText, String regEndTimeText) {

        // Step 1: Convert all raw inputs into the required data types.
        Timestamp eventStartTimestamp = getTimestampFromCalendar(eventStartCalendar, startDateText, startTimeText);
        Timestamp eventEndTimestamp = getTimestampFromCalendar(eventEndCalendar, endDateText, endTimeText);
        Timestamp regStartTimestamp = getTimestampFromCalendar(registrationStartCalendar, regStartDateText, regStartTimeText);
        Timestamp regEndTimestamp = getTimestampFromCalendar(registrationEndCalendar, regEndDateText, regEndTimeText);

        Double price = priceStr.isEmpty() ? null : Double.parseDouble(priceStr);
        Integer capacity = maxAttendeesStr.isEmpty() ? null : Integer.parseInt(maxAttendeesStr);
        Integer waitingListLimit = waitingListLimitStr.isEmpty() ? null : Integer.parseInt(waitingListLimitStr);

        // Step 2: Validate all the processed inputs using the internal business logic.
        String validationError = validateEventInput(eventName, startDateText, startTimeText,
                endDateText, endTimeText, eventStartTimestamp, eventEndTimestamp,
                regStartDateText, regStartTimeText, regEndDateText, regEndTimeText,
                regStartTimestamp, regEndTimestamp, capacity, waitingListLimit);

        if (validationError != null) {
            return validationError; // Validation failed, return the error message immediately.
        }

        // Step 3: If validation succeeds, assemble the final Event object.
        Event newEvent = new Event();
        newEvent.setName(eventName);
        newEvent.setDescription(description);
        newEvent.setLocation(location);
        newEvent.setPrice(price);
        newEvent.setCapacity(capacity);
        newEvent.setWaitingListLimit(waitingListLimit);
        newEvent.setLotteryGuidelines(lotteryGuidelinesStr);
        newEvent.setGeoLocationRequired(isGeoLocationRequired);
        newEvent.setEventStartDateTime(eventStartTimestamp);
        newEvent.setEventEndDateTime(eventEndTimestamp);
        newEvent.setRegistrationStartDateTime(regStartTimestamp);
        newEvent.setRegistrationEndDateTime(regEndTimestamp);
        newEvent.setStatus("open");
        // Set the creation timestamp for sorting purposes in the repository.
        newEvent.setCreatedAt(new Timestamp(new Date()));

        // Step 4: Pass the fully formed object to the repository to be saved.
        eventRepository.createEvent(newEvent);

        return null; // Signal that the process was started successfully.
    }

    /**
     * The validation logic
     *
     * @return A String containing an error message if validation fails, otherwise null.
     */
    private String validateEventInput(String eventName, String startDateText, String startTimeText,
                                      String endDateText, String endTimeText,
                                      Timestamp eventStartTimestamp, Timestamp eventEndTimestamp,
                                      String regStartDateText, String regStartTimeText,
                                      String regEndDateText, String regEndTimeText,
                                      Timestamp regStartTimestamp, Timestamp regEndTimestamp,
                                      Integer capacity, Integer waitingListLimit) {

        if (eventName.isEmpty()) {
            return "Event name is required.";
        }

        // If a registration start date is set, an end date must also be set
        boolean isRegStartDateSet = !regStartDateText.isEmpty();
        boolean isRegEndDateSet = !regEndDateText.isEmpty();
        if (isRegStartDateSet && !isRegEndDateSet) {
            return "A registration end date is required if a start date is set.";
        }

        // All dates require a time or the Firestore timestamp will be null
        boolean isEventStartDateSet = !startDateText.isEmpty();
        boolean isEventStartTimeSet = !startTimeText.isEmpty();
        if (isEventStartDateSet && !isEventStartTimeSet) {
            return "Please select a start time for the selected event start date.";
        }

        boolean isEventEndDateSet = !endDateText.isEmpty();
        boolean isEventEndTimeSet = !endTimeText.isEmpty();
        if (isEventEndDateSet && !isEventEndTimeSet) {
            return "Please select an end time for the selected event end date.";
        }

        boolean isRegStartTimeSet = !regStartTimeText.isEmpty();
        if (isRegStartDateSet && !isRegStartTimeSet) {
            return "Please select a start time for the selected registration start date.";
        }

        boolean isRegEndTimeSet = !regEndTimeText.isEmpty();
        if (isRegEndDateSet && !isRegEndTimeSet) {
            return "Please select an end time for the selected registration end date.";
        }

        // Start before end checks:
        if (eventStartTimestamp != null && eventEndTimestamp != null && eventStartTimestamp.compareTo(eventEndTimestamp) >= 0) {
            return "Event start time must be before event end time.";
        }

        if (regStartTimestamp != null && regEndTimestamp != null && regStartTimestamp.compareTo(regEndTimestamp) >= 0) {
            return "Registration start time must be before registration end time.";
        }

        // Check that registration period is before the event starts
        if (regEndTimestamp != null && eventStartTimestamp != null && regEndTimestamp.compareTo(eventStartTimestamp) > 0) {
            return "Registration must end before the event starts.";
        }

        // Date/time is in the future checks:
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

        if (regStartTimestamp != null && regStartTimestamp.compareTo(now) < 0) {
            return "Registration start time cannot be in the past.";
        }

        if (eventStartTimestamp != null && eventStartTimestamp.compareTo(now) < 0) {
            return "Event start time cannot be in the past.";
        }

        if (waitingListLimit != null && capacity == null) {
            return "A capacity is required to set a waiting list limit.";
        }

        if (capacity != null && capacity < 0) {
            return "Capacity cannot be negative.";
        }

        if (waitingListLimit != null && waitingListLimit < 1) {
            return "Waiting list limit cannot be smaller than one.";
        }

        if (waitingListLimit != null && capacity != null && waitingListLimit < capacity) {
            return "Waiting list size must be greater than or equal to the number of attendees.";
        }

        return null; // Validation successful
    }

    /**
     * Helper method to create a Firestore Timestamp from a Calendar.
     * This logic is moved from the Fragment as it's a data conversion task.
     *
     * @return A Timestamp object, or null if date or time is missing.
     */
    private Timestamp getTimestampFromCalendar(Calendar calendar, String dateText, String timeText) {
        if (dateText.isEmpty() || timeText.isEmpty()) {
            return null;
        }
        return new Timestamp(calendar.getTime());
    }
}
