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
    private final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> _isRunDrawButtonEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();

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

    /**
     * Simulates fetching data. Instead of creating a hardcoded event, this method now
     * uses the pre-configured state set by the test's Arrange block.
     */
    @Override
    public void fetchEventAndCapacityStatus(String eventId) {
        _isLoading.postValue(true);

        // Post the values that were set by the test method.
        _event.postValue(eventToReturn);
        _isRunDrawButtonEnabled.postValue(buttonEnabled);

        _isLoading.postValue(false);
    }

    /**
     * No-op implementation for updating the event poster.
     * Fragment tests don't need to inspect this behavior; they only
     * require the method to exist so the interface compiles.
     *
     * @param eventId The ID of the event.
     * @param posterBase64 The Base64-encoded poster image.
     */
    @Override
    public void updateEventPoster(String eventId, String posterBase64) {
        // No-op for fragment tests
    }
}
     * Simulates finalizing an event.
     * @param eventId the ID of the event to finalize
     */
    @Override
    public void finalizeEvent(String eventId) {
        wasFinalizeCalled = true;

        // Simulate the logic of updating the event status
        if (eventToReturn != null) {
            eventToReturn.setStatus("finalized");
            _event.postValue(eventToReturn);
            _message.postValue("Event finalized successfully!");
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
        _entrants.postValue(entrantsToReturn);
    }
}
