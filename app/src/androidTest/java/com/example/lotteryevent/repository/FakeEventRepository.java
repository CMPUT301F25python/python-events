package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.lotteryevent.data.Event;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation of IEventRepository for testing purposes.
 */
public class FakeEventRepository implements IEventRepository {

    // --- LiveData to be observed by the ViewModel ---
    private final MutableLiveData<List<Event>> _events = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();

    // --- In-memory "database" ---
    // This list holds the state of our fake repository.
    private final ArrayList<Event> inMemoryEvents = new ArrayList<>();

    // --- Test control flags ---
    private boolean shouldReturnError = false;

    /**
     * Initializes the fake repository with some default data.
     */
    public FakeEventRepository() {
        // Populate our in-memory list with initial data.
        Event event1 = new Event();
        event1.setName("Event 1");
        event1.setCreatedAt(Timestamp.now());
        inMemoryEvents.add(event1);

        Event event2 = new Event();
        event2.setName("Event 2");
        event2.setCreatedAt(Timestamp.now());
        inMemoryEvents.add(event2);

        // Post the initial state to the LiveData.
        _events.postValue(new ArrayList<>(inMemoryEvents));
    }

    @Override
    public LiveData<List<Event>> getUserEvents() {
        return _events;
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
     * Simulates fetching events by posting the current state of the in-memory list.
     */
    @Override
    public void fetchUserEvents() {
        _isLoading.postValue(true);

        if (shouldReturnError) {
            _message.postValue("Test Error: Could not fetch events.");
            _events.postValue(new ArrayList<>()); // Post an empty list on error
        } else {
            // Post a copy of the current in-memory list.
            _events.postValue(new ArrayList<>(inMemoryEvents));
        }

        _isLoading.postValue(false);
    }

    /**
     * Simulates creating a new event by adding it to the in-memory list
     * and updating the LiveData to notify observers of the change.
     * @param event The new event to add.
     */
    @Override
    public void createEvent(Event event) {
        _isLoading.postValue(true);

        if (shouldReturnError) {
            _message.postValue("Test Error: Could not create event.");
            _isLoading.postValue(false);
            return;
        }

        inMemoryEvents.add(event);
        _events.postValue(new ArrayList<>(inMemoryEvents));

        _message.postValue("Event created successfully!");
        _isLoading.postValue(false);
    }


    public void setShouldReturnError(boolean value) {
        this.shouldReturnError = value;
    }

    /**
     * A helper for tests to check the internal state of the fake repository.
     * @return The current list of in-memory events.
     */
    public List<Event> getInMemoryEvents() {
        return inMemoryEvents;
    }
}