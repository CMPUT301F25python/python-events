package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-double implementation of IAvailableEventsRepository.
 *
 * This fake does NOT talk to Firebase. Instead, it:
 *  - Exposes LiveData just like the real repository
 *  - Allows tests to preconfigure the events or an error
 *  - Records whether fetchAvailableEvents() / removeListener() were called
 */
public class FakeAvailableEventsRepository implements IAvailableEventsRepository {

    // LiveData exposed to ViewModels (same contract as real repo)
    private final MutableLiveData<List<Event>> eventsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> messageLiveData = new MutableLiveData<>();

    // Stub configuration for tests
    private List<Event> eventsToReturn = new ArrayList<>();
    private boolean shouldReturnError = false;
    private String errorMessage = "Failed to load events. Please check your connection.";

    // Flags so tests can assert interactions
    private boolean fetchCalled = false;
    private boolean removeListenerCalled = false;

    @Override
    public LiveData<List<Event>> getAvailableEvents() {
        return eventsLiveData;
    }

    @Override
    public LiveData<Boolean> isLoading() {
        return loadingLiveData;
    }

    @Override
    public LiveData<String> getMessage() {
        return messageLiveData;
    }

    /**
     * Simulates a single fetch from the backend.
     *
     * Behavior:
     *  - Sets isLoading to true, then false
     *  - If shouldReturnError == true: emits errorMessage and an empty list
     *  - Otherwise: emits eventsToReturn and clears any previous message
     */
    @Override
    public void fetchAvailableEvents() {
        fetchCalled = true;

        loadingLiveData.setValue(true);

        if (shouldReturnError) {
            loadingLiveData.setValue(false);
            messageLiveData.setValue(errorMessage);
            eventsLiveData.setValue(new ArrayList<>()); // empty list on error
        } else {
            loadingLiveData.setValue(false);
            messageLiveData.setValue(null); // clear any previous message
            eventsLiveData.setValue(new ArrayList<>(eventsToReturn));
        }
    }

    /**
     * In the real repo this removes the Firebase listener.
     * Here we just record that it was called so tests can assert on it.
     */
    @Override
    public void removeListener() {
        removeListenerCalled = true;
    }

    // ---------- Helper methods for tests ----------

    /**
     * Configure the list of events that will be emitted on a successful fetch.
     */
    public void setEventsToReturn(List<Event> events) {
        if (events == null) {
            this.eventsToReturn = new ArrayList<>();
        } else {
            this.eventsToReturn = new ArrayList<>(events);
        }
    }

    /**
     * Configure the repository to return an error on the next fetch.
     */
    public void setError(String message) {
        this.shouldReturnError = true;
        if (message != null) {
            this.errorMessage = message;
        }
    }

    /**
     * Configure the repository to return success on fetch (default).
     */
    public void clearError() {
        this.shouldReturnError = false;
        this.errorMessage = "Failed to load events. Please check your connection.";
    }

    /**
     * For tests: check whether fetchAvailableEvents() was invoked.
     */
    public boolean wasFetchCalled() {
        return fetchCalled;
    }

    /**
     * For tests: check whether removeListener() was invoked.
     */
    public boolean wasRemoveListenerCalled() {
        return removeListenerCalled;
    }

    /**
     * For tests: manually push events as if there was a snapshot update *after* initial fetch.
     */
    public void emitEvents(List<Event> events) {
        if (events == null) {
            eventsLiveData.setValue(new ArrayList<>());
        } else {
            eventsLiveData.setValue(new ArrayList<>(events));
        }
    }

    /**
     * For tests: manually push an error message at any time.
     */
    public void emitErrorMessage(String message) {
        messageLiveData.setValue(message);
    }

    /**
     * For tests: manually set loading state.
     */
    public void emitLoading(boolean isLoading) {
        loadingLiveData.setValue(isLoading);
    }
}
