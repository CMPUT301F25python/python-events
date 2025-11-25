package com.example.lotteryevent.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation of IOrganizerEventRepository for testing purposes.
 * This class allows us to control the data and states returned to the ViewModel,
 * enabling isolated unit tests without hitting a real database.
 */
public class FakeOrganizerEventRepository implements IOrganizerEventRepository {

    // MutableLiveData fields that we can control within this fake class.
    private final MutableLiveData<Event> _event = new MutableLiveData<>();
    private final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> _isRunDrawButtonEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();
    private boolean shouldReturnError = false;


    // --- Test Control Properties ---
    // These fields hold the state that the test methods will configure.
    private Event eventToReturn = new Event(); // Start with a default, non-null event.
    private boolean buttonEnabled = true;
    private List<Entrant> entrantsToReturn = new ArrayList<>();
    public boolean wasFinalizeCalled = false;
    public boolean wasFetchEntrantsCalled = false;

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

    public void setEntrantsToReturn(List<Entrant> entrants) {
        this.entrantsToReturn = entrants;
        _entrants.postValue(entrants);
    }

    public void setShouldReturnError(boolean shouldReturnError) {
        this.shouldReturnError = shouldReturnError;
    }


    // --- Implementation of IOrganizerEventRepository ---

    @Override
    public LiveData<Event> getEvent() {
        return _event;
    }
    @Override
    public LiveData<List<Entrant>> getEntrants() { return _entrants; }

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

    @Override
    public void fetchEventAndCapacityStatus(String eventId) {
        // 1. Simulate the start of a data fetch.
        _isLoading.postValue(true);

        // 2. Check our flag to decide whether to return data or an error.
        if (shouldReturnError) {
            _message.postValue("Test Error: Could not fetch event.");
            _event.postValue(null); // Post null on error to clear previous data.
            _isRunDrawButtonEnabled.postValue(false);
        } else {
            // Create a mock Event object for the success scenario.
            Event testEvent = new Event();
            testEvent.setEventId("test-id-123");
            testEvent.setName("Test Event");
            testEvent.setCapacity(100);
            testEvent.setStatus("open");

            _event.postValue(testEvent);
            // Simulate the event not being at capacity.
            _isRunDrawButtonEnabled.postValue(true);
        }

        // 3. Simulate the end of the data fetch.
        _isLoading.postValue(false);
    }

    /**
     * Simulates finalizing an event.
     * @param eventId the ID of the event to finalize
     */
    @Override
    public void finalizeEvent(String eventId) {
        wasFinalizeCalled = true;

        // Simulate the logic of updating the event status
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
        // Post the pre-configured entrants
        _entrants.setValue(entrantsToReturn);
    }
}