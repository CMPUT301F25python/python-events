package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation of {@link IEntrantListRepository} for testing purposes.
 * Holds entrants in memory and records notification calls.
 */
public class FakeEntrantListRepository implements IEntrantListRepository {

    private final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>();

    // In-memory list of entrants to return
    private List<Entrant> inMemoryEntrants = new ArrayList<>();

    // Notification tracking for tests
    public static class NotificationCall {
        public final String uid;
        public final String eventId;
        public final String message;

        public NotificationCall(String uid, String eventId, String message) {
            this.uid = uid;
            this.eventId = eventId;
            this.message = message;
        }
    }

    private final List<NotificationCall> notificationCalls = new ArrayList<>();

    // Test control flag
    private boolean shouldReturnError = false;

    public FakeEntrantListRepository() {
        // Default: start with empty list
        _entrants.postValue(inMemoryEntrants);
    }

    @Override
    public LiveData<List<Entrant>> fetchEntrantsByStatus(String eventId, String status) {
        if (shouldReturnError) {
            _entrants.postValue(null); // Fragment treats null as "failed to load"
        } else {
            _entrants.postValue(inMemoryEntrants);
        }
        return _entrants;
    }

    @Override
    public void notifyEntrant(String uid, String eventId, String organizerMessage) {
        notificationCalls.add(new NotificationCall(uid, eventId, organizerMessage));
    }

    // --- Test helpers ---

    public void setEntrants(List<Entrant> entrants) {
        this.inMemoryEntrants = entrants != null ? entrants : new ArrayList<>();
        this._entrants.postValue(this.inMemoryEntrants);
    }

    public List<NotificationCall> getNotificationCalls() {
        return notificationCalls;
    }
}
