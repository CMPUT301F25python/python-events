package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation of IOrganizerEventRepository for testing purposes.
 * This class allows us to control the data and states returned to the ViewModel,
 * enabling isolated unit tests and complex UI tests without hitting a real database.
 */
public class FakeOrganizerEventRepository implements IOrganizerEventRepository {

    // MutableLiveData fields that we can control within this fake class.
    private final MutableLiveData<Event> _event = new MutableLiveData<>();
    private final MutableLiveData<List<Entrant>> _entrants =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> _isRunDrawButtonEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();

    // --- Test Control Properties ---
    // These fields hold the state that the test methods will configure.
    private Event eventToReturn = createDefaultEvent();
    private boolean buttonEnabled = true;
    private boolean shouldReturnError = false;
    private List<Entrant> entrantsToReturn = new ArrayList<>();
    public boolean wasFinalizeCalled = false;
    public boolean wasFetchEntrantsCalled = false;

    // --- Poster update tracking for tests ---
    private String lastUpdatedPosterEventId;
    private String lastUpdatedPosterBase64;

    /**
     * Creates a default event used for the "successful" path in tests.
     * @return A simple Event instance with a known name.
     */
    private Event createDefaultEvent() {
        Event e = new Event();
        e.setName("Test Event");
        return e;
    }

    // --- Public methods for test setup ---

    /**
     * Sets the specific Event object that will be returned when fetch is called.
     * This is used to test different event states (open, finalized, upcoming).
     * @param event The Event object to post to the LiveData stream.
     */
    public void setEventToReturn(Event event) {
        this.eventToReturn = event;
    }

    /**
     * Sets the state for the 'Run Draw' button.
     * This is used to test the UI when an event is at capacity.
     * @param isEnabled The boolean state to post to the LiveData stream.
     */
    public void setButtonEnabled(boolean isEnabled) {
        this.buttonEnabled = isEnabled;
    }

    /**
     * Configures whether this fake should simulate an error when fetching
     * the event and capacity status.
     * @param shouldReturnError True to simulate an error; false for normal success behavior.
     */
    public void setShouldReturnError(boolean shouldReturnError) {
        this.shouldReturnError = shouldReturnError;
    }

    public void setEntrantsToReturn(List<Entrant> entrants) {
        this.entrantsToReturn = entrants;
        _entrants.setValue(entrants);
    }

    /**
     * Returns the last event ID passed to updateEventPoster.
     * Used by tests to verify that the ViewModel delegates correctly.
     * @return The last event ID passed to updateEventPoster.
     */
    public String getLastUpdatedPosterEventId() {
        return lastUpdatedPosterEventId;
    }

    /**
     * Returns the last Base64 string passed to updateEventPoster.
     * Used by tests to verify that the ViewModel delegates correctly.
     * @return The last Base64 poster string passed to updateEventPoster.
     */
    public String getLastUpdatedPosterBase64() {
        return lastUpdatedPosterBase64;
    }

    // --- Implementation of IOrganizerEventRepository ---

    @Override
    public LiveData<Event> getEvent() {
        return _event;
    }

    @Override
    public LiveData<List<Entrant>> getEntrants() {
        return _entrants;
    }

    @Override
    public LiveData<Boolean> isRunDrawButtonEnabled() {
        return _isRunDrawButtonEnabled;
    }

    @Override
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    @Override
    public LiveData<String> getMessage() {
        return _message;
    }

    /**
     * Simulates fetching data. If {@code shouldReturnError} is true,
     * posts an error state. Otherwise, posts the configured event and
     * button-enabled state.
     */
    @Override
    public void fetchEventAndCapacityStatus(String eventId) {
        _isLoading.setValue(true);

        if (shouldReturnError) {
            _event.setValue(null);
            _isRunDrawButtonEnabled.setValue(false);
            _message.setValue("Test Error: Could not fetch event.");
        } else {
            _event.setValue(eventToReturn);
            _isRunDrawButtonEnabled.setValue(buttonEnabled);
            _message.setValue(null);
        }

        _isLoading.setValue(false);
    }

    /**
     * Simulates updating the event poster for a given event.
     * Instead of writing to a real database, this method just records
     * the arguments so tests can verify that the ViewModel called it.
     *
     * @param eventId The ID of the event whose poster is being updated.
     * @param posterBase64 The Base64-encoded poster image data.
     */
    @Override
    public void updateEventPoster(String eventId, String posterBase64) {
        this.lastUpdatedPosterEventId = eventId;
        this.lastUpdatedPosterBase64 = posterBase64;
    }

    /**
     * Simulates finalizing an event.
     * @param eventId the ID of the event to finalize
     */
    @Override
    public void finalizeEvent(String eventId) {
        wasFinalizeCalled = true;

        if (eventToReturn != null) {
            eventToReturn.setStatus("finalized");
            _event.setValue(eventToReturn);
            _message.setValue("Event finalized successfully!");
        }
    }

    /**
     * Simulates fetching entrants.
     * @param eventId the ID of the event
     */
    @Override
    public void fetchEntrants(String eventId) {
        wasFetchEntrantsCalled = true;
        _entrants.setValue(entrantsToReturn);
    }
}
