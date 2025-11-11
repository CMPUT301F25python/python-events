package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.lotteryevent.data.Event;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation of IEventRepository for testing purposes.
 * This class allows us to control the data and state returned to the ViewModel
 * without any actual network or database calls.
 */
public class FakeEventRepository implements IEventRepository {

    // These are the LiveData objects the ViewModel will observe.
    private final MutableLiveData<List<Event>> events = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    // A flag to control whether this fake should return an error.
    private boolean shouldReturnError = false;

    public void setShouldReturnError(boolean value) {
        this.shouldReturnError = value;
    }

    @Override
    public LiveData<List<Event>> getUserEvents() {
        return events;
    }

    @Override
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    @Override
    public LiveData<String> getError() {
        return error;
    }

    @Override
    public void fetchUserEvents() {
        isLoading.setValue(true);

        if (shouldReturnError) {
            error.setValue("Test Error: Could not fetch events.");
            events.setValue(new ArrayList<>()); // Post an empty list on error
        } else {
            // Create and post a hardcoded list of fake events.
            ArrayList<Event> fakeEvents = new ArrayList<>();
            Event event1 = new Event();
            event1.setName("Event 1");
            event1.setCreatedAt(Timestamp.now());
            fakeEvents.add(event1);
            Event event2 = new Event();
            event2.setName("Event 2");
            event2.setCreatedAt(Timestamp.now());
            fakeEvents.add(event2);
            events.setValue(fakeEvents);
        }

        isLoading.setValue(false);
    }
}