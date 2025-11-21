package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation of {@link IEntrantListRepository}.
 * This fake repository allows unit tests to control entrant data, simulate
 * success or failure conditions, and inspect notification calls without
 * requiring a real Firestore backend.
 * <p>All data is stored in memory and exposed through LiveData so that
 * ViewModels and fragments can be tested under normal lifecycle conditions.</p>
 */
public class FakeEntrantListRepository implements IEntrantListRepository {

    private final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>();

    // In-memory list of entrants to return
    private List<Entrant> inMemoryEntrants = new ArrayList<>();

    /**
     * Data structure representing a single notification dispatched during a test.
     * Used to record and inspect calls to so tests can verify correct behavior.
     */
    public static class NotificationCall {
        public final String uid;
        public final String eventId;
        public final String message;

        /**
         * Creates a record of a notification operation performed by the fake repository.
         * @param uid the unique identifier of the entrant who would have received the notification
         * @param eventId the event identifier associated with the notification
         * @param message the message content passed to the repository
         */
        public NotificationCall(String uid, String eventId, String message) {
            this.uid = uid;
            this.eventId = eventId;
            this.message = message;
        }
    }

    private final List<NotificationCall> notificationCalls = new ArrayList<>();

    // Test control flag
    private boolean shouldReturnError = false;

    /**
     * Creates a new fake repository instance with an empty in-memory entrant list.
     * Initializes the internal LiveData so observers can receive updates during testing.
     */
    public FakeEntrantListRepository() {
        // Default: start with empty list
        _entrants.postValue(inMemoryEntrants);
    }

    /**
     * Simulates fetching entrants by status for a given event. Returns the
     * in-memory list of entrants unless {@code shouldReturnError} is enabled, in
     * which case {@code null} is posted to simulate a loading failure.
     * @param eventId the identifier of the event (ignored in this fake implementation)
     * @param status the entrant status filter (ignored in this fake implementation)
     * @return a LiveData object containing the in-memory entrant list or null on error
     */
    @Override
    public LiveData<List<Entrant>> fetchEntrantsByStatus(String eventId, String status) {
        if (shouldReturnError) {
            _entrants.postValue(null); // Fragment treats null as "failed to load"
        } else {
            _entrants.postValue(inMemoryEntrants);
        }
        return _entrants;
    }

    /**
     * Records a simulated notification call for later inspection. No actual
     * notification is sent. Tests can retrieve the recorded calls via
     * {@link #getNotificationCalls()}.
     * @param uid the user ID of the entrant who would receive the notification
     * @param eventId the ID of the associated event
     * @param organizerMessage the message content intended for the entrant
     */
    @Override
    public void notifyEntrant(String uid, String eventId, String organizerMessage) {
        notificationCalls.add(new NotificationCall(uid, eventId, organizerMessage));
    }

    /**
     * Replaces the in-memory entrant list with the provided value and posts it to
     * LiveData so any observers receive an update. Passing {@code null} resets the
     * list to empty.
     * @param entrants the new list of entrants to store in memory, or null to clear the list
     */
    public void setEntrants(List<Entrant> entrants) {
        this.inMemoryEntrants = entrants != null ? entrants : new ArrayList<>();
        this._entrants.postValue(this.inMemoryEntrants);
    }

    /**
     * Returns all recorded notification calls since repository creation or since
     * the last test reset. Useful for verifying that the correct users were notified
     * with the correct message content.
     * @return a list of {@link NotificationCall} objects representing dispatched notifications
     */
    public List<NotificationCall> getNotificationCalls() {
        return notificationCalls;
    }
}
