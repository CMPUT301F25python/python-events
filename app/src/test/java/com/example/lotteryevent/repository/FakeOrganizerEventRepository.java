package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.lotteryevent.data.Event;

/**
 * A fake implementation of IOrganizerEventRepository for testing purposes.
 * This class allows us to control the data and states returned to the ViewModel,
 * enabling isolated unit tests without hitting a real database.
 */
public class FakeOrganizerEventRepository implements IOrganizerEventRepository {

    // MutableLiveData fields that we can control within this fake class.
    private final MutableLiveData<Event> _event = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isRunDrawButtonEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();
    private boolean shouldReturnError = false;

    /**
     * Sets a flag to determine if the next fetch should simulate an error.
     * @param value true to simulate an error, false to simulate success.
     */
    public void setShouldReturnError(boolean value) {
        shouldReturnError = value;
    }

    // --- Implementation of IOrganizerEventRepository ---

    @Override
    public LiveData<Event> getEvent() {
        return _event;
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
}