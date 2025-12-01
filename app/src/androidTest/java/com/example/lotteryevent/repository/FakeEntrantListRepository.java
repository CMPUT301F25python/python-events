package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Notification;

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

    // user-facing message
    private final MutableLiveData<String> _userMessage = new MutableLiveData<>();

    /**
     * In-memory list of notifications recorded during tests.
     * Uses the shared {@link Notification} POJO instead of a custom helper class.
     */
    private final List<Notification> notificationCalls = new ArrayList<>();

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
     * Displays the live message to users
     * @return string, _userMessage
     */
    @Override
    public LiveData<String> getUserMessage() {
        return _userMessage;
    }

    /**
     * Allows organizer to write a custom message
     * @param message set by the organizer
     */
    @Override
    public void setUserMessage(String message) {
        _userMessage.postValue(message);
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
            _userMessage.postValue("Failed to load entrants.");
        } else {
            _entrants.postValue(inMemoryEntrants);
        }
        return _entrants;
    }

    /**
     * In this case, identical to {@link #fetchEntrantsByStatus(String, String)}
     * @param eventId the unique identifier of the event for which entrants are
     *                being fetched
     * @return a LiveData object containing the in-memory entrant list or null on error
     */
    @Override
    public LiveData<List<Entrant>> fetchAllEntrants(String eventId) {
        if (shouldReturnError) {
            _entrants.postValue(null);
            _userMessage.postValue("Failed to load entrants.");
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
        if (uid == null || eventId == null) {
            _userMessage.postValue("Failed to send notification.");
            return;
        }

        Notification notification = new Notification();
        notification.setRecipientId(uid);
        notification.setEventId(eventId);
        notification.setMessage(organizerMessage);
        notificationCalls.add(notification);

        _userMessage.postValue(null);
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
        _userMessage.postValue(null); // clear any messages/errors
    }

    /**
     * Returns all recorded notification calls since repository creation or since
     * the last test reset. Useful for verifying that the correct users were notified
     * with the correct message content.
     * @return a list of {@link Notification} objects representing dispatched notifications
     */
    public List<Notification> getNotificationCalls() {
        return notificationCalls;
    }

    /**
     * Simulates updating an entrant's status in the in-memory list.
     * If {@code shouldReturnError} is true, calls {@code onFailure}.
     * Otherwise, finds the entrant, updates the status, refreshes the LiveData, and calls {@code onSuccess}.
     *
     * @param eventId   The event ID (ignored in fake)
     * @param userId    The ID of the entrant to update
     * @param newStatus The new status string
     * @param callback  Callback to notify ViewModel of success/failure
     */
    @Override
    public void updateEntrantStatus(String eventId, String userId, String newStatus, StatusUpdateCallback callback, boolean sendNotif) {
        if (shouldReturnError) {
            if (callback != null) {
                /**
                 * Throws exception on failure
                 */
                callback.onFailure(new Exception("Simulated database failure"));
            }
            return;
        }

        boolean found = false;
        for (Entrant e : inMemoryEntrants) {
            if (e.getUserId() != null && e.getUserId().equals(userId)) {
                e.setStatus(newStatus);
                found = true;
                break;
            }
        }

        if (found) {
            // Trigger LiveData update to reflect the change in UI immediately
            _entrants.postValue(inMemoryEntrants);
            if (callback != null) {
                callback.onSuccess();
            }
        } else {
            if (callback != null) {
                /**
                 * Throws exception on failure
                 */
                callback.onFailure(new Exception("User ID not found in fake data"));
            }
        }
    }
}
