package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.lotteryevent.data.Entrant;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation of EntrantListRepository for testing purposes.
 * Simulates fetching entrants and sending notifications.
 */
public class FakeEntrantListRepository {

    private final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>();
    private final ArrayList<Entrant> inMemoryEntrants = new ArrayList<>();
    private boolean shouldReturnError = false;
    private boolean notificationSent = false;

    public FakeEntrantListRepository() {
        // Add some default test entrants
        Entrant e1 = new Entrant();
        e1.setUserId("user1");
        e1.setStatus("accepted");
        inMemoryEntrants.add(e1);

        Entrant e2 = new Entrant();
        e2.setUserId("user2");
        e2.setStatus("waiting");
        inMemoryEntrants.add(e2);

        _entrants.postValue(new ArrayList<>(inMemoryEntrants));
    }

    public LiveData<List<Entrant>> fetchEntrantsByStatus(String eventId, String status) {
        if (shouldReturnError) {
            _entrants.postValue(new ArrayList<>());
        } else {
            List<Entrant> filtered = new ArrayList<>();
            for (Entrant e : inMemoryEntrants) {
                if (status.equals(e.getStatus())) {
                    filtered.add(e);
                }
            }
            _entrants.postValue(filtered);
        }
        return _entrants;
    }

    public void notifyEntrant(String uid, String eventId, String organizerMessage) {
        if (!shouldReturnError) {
            notificationSent = true;
        }
    }

    public boolean wasNotificationSent() {
        return notificationSent;
    }

    public void setShouldReturnError(boolean value) {
        shouldReturnError = value;
    }

    public List<Entrant> getInMemoryEntrants() {
        return inMemoryEntrants;
    }
}
