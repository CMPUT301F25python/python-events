package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A fake implementation of IEventRepository for testing purposes.
 */
public class FakeEventRepository implements IEventRepository {

    // --- LiveData to be observed by the ViewModel ---
    private final MutableLiveData<List<Event>> _events = new MutableLiveData<>();
    private final MutableLiveData<Event> _event = new MutableLiveData<>();
    private final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>();
    private final MutableLiveData<Integer> _waitingListCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> _selectedUsersCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> _availableSpaceCount = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();

    // --- In-memory "database" ---
    // These lists hold the state of our fake repository.
    private ArrayList<Event> inMemoryEvents = new ArrayList<>();
    private final ArrayList<Entrant> inMemoryEntrants = new ArrayList<>();

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
        event1.setEventId("fake-event-id");
        event1.setCapacity(2);
        inMemoryEvents.add(event1);

        Event event2 = new Event();
        event2.setName("Event 2");
        event2.setCreatedAt(Timestamp.now());
        inMemoryEvents.add(event2);

        Entrant entrant1 = new Entrant();
        entrant1.setUserId("1");
        entrant1.setUserName("Bob");
        entrant1.setStatus("waiting");
        inMemoryEntrants.add(entrant1);

        Entrant entrant2 = new Entrant();
        entrant2.setUserId("2");
        entrant2.setUserName("Joe");
        entrant2.setStatus("waiting");
        inMemoryEntrants.add(entrant2);

        // Post the initial state to the LiveData.
        _events.postValue(new ArrayList<>(inMemoryEvents));
    }

    @Override
    public LiveData<List<Event>> getUserEvents() {
        return _events;
    }

    @Override
    public LiveData<Event> getUserEvent() { return _event; }

    @Override
    public LiveData<List<Entrant>> getEventEntrants() {return _entrants; }

    @Override
    public LiveData<Integer> getWaitingListCount() { return _waitingListCount; }

    @Override
    public LiveData<Integer> getSelectedUsersCount() { return _selectedUsersCount; }

    @Override
    public LiveData<Integer> getAvailableSpaceCount() { return _availableSpaceCount; }

    @Override
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    @Override
    public LiveData<String> getMessage() {
        return _message;
    }

    /**
     * Helper method for testing
     * @param value value to set loading to
     */
    public void setIsLoading(boolean value) {
        _isLoading.setValue(value);
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
     * Simulates fetching event given event ID and its entrants by posting the
     * current state of the in-memory list.
     */
    @Override
    public void fetchEventAndEntrants(String eventId) {
        _isLoading.postValue(true);

        if (shouldReturnError) {
            _message.postValue("Test Error: Could not fetch event.");
            _event.postValue(null); // Post an empty list on error
        } else {
            // Post a copy of the current first event in in-memory list.
            Event event = null;
            for (Event e : inMemoryEvents) {
                if (Objects.equals(e.getEventId(), eventId)) {
                    event = e;
                    break;
                }
            }
            if (event == null) {
                throw new RuntimeException("Event with given ID cannot be found!");
            }
            _event.postValue(event);

            // Fetch entrants, (fetchEntrantsTask())
            _entrants.postValue(inMemoryEntrants);

            // Fetch entrant counts (fetchEntrantsCountsTask())
            int waitingListCount = 0;
            int selectedUsersCount = 0;
            for (Entrant entrant : inMemoryEntrants) {
                if (Objects.equals(entrant.getStatus(), "waiting")) {
                    waitingListCount++;
                } else if (Objects.equals(entrant.getStatus(), "invited")) {
                    selectedUsersCount++;
                }
            }
            _waitingListCount.postValue(waitingListCount);
            _selectedUsersCount.postValue(selectedUsersCount);
            if (event.getCapacity() == null) {
                _availableSpaceCount.postValue(null);
            } else {
                _availableSpaceCount.postValue(event.getCapacity() - selectedUsersCount);
            }
        }

        _isLoading.postValue(false);
    }

    /**
     * Simulates updating attribute of entrants who have a specified old value
     * @param eventId event to access its entrants
     * @param fieldName attribute of entrants to modify
     * @param oldValue old value for only updating specific entrants
     * @param newValue new value to set
     */
    @Override
    public void updateEntrantsAttributes(String eventId, String fieldName, Object oldValue, Object newValue) {
        for (Entrant entrant : inMemoryEntrants) {
            // add the other fields as needed
            if (Objects.equals(fieldName, "status") && Objects.equals(entrant.getStatus(), String.valueOf(oldValue))) {
                entrant.setStatus(String.valueOf(newValue));
            }
        }
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

    /**
     * Sets whether to set a return error
     * @param value set return error boolean
     */
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

    /**
     * A helper for tests to set events used by tests
     * @param events list of events to set
     */
    public void setInMemoryEvents(List<Event> events) {
        inMemoryEvents = (ArrayList<Event>) events;
    }
}