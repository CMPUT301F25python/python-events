package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.Notification;
import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation of {@link INotificationRepository} designed for UI testing.
 * <p>
 * This class avoids network calls to Firestore by maintaining a local in-memory list of
 * {@link Notification} objects. It allows tests to inject specific data scenarios
 * using {@link #setNotifications(List)} and verify state changes without flakiness.
 */
public class FakeNotificationRepository implements INotificationRepository {

    private final MutableLiveData<List<Notification>> notificationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Notification>> notificationsForEventLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> messageLiveData = new MutableLiveData<>();

    // Helper list to modify data easily during tests
    private final List<Notification> inMemoryList = new ArrayList<>();
    private String lastEventId = null;
    @Override
    public LiveData<List<Notification>> getNotifications() {
        return notificationsLiveData;
    }

    @Override
    public LiveData<Boolean> isLoading() {
        return isLoadingLiveData;
    }

    @Override
    public LiveData<String> getMessage() {
        return messageLiveData;
    }

    @Override
    public LiveData<List<Notification>> getNotificationsForEvent() { return notificationsForEventLiveData; }

    /**
     * Simulates marking a notification as seen by updating the in-memory object.
     * <p>
     * This method finds the notification in the local list, updates its status,
     * and refreshes the LiveData observers to trigger a UI update immediately.
     *
     * @param notificationId            The ID of the notification to mark as seen.
     * @param notificationCustomManager The manager context (unused in this fake implementation).
     */
    @Override
    public void markNotificationAsSeen(String notificationId, NotificationCustomManager notificationCustomManager) {
        // Iterate through our in-memory list to find the item
        for (Notification n : inMemoryList) {
            if (n.getNotificationId().equals(notificationId)) {
                n.setSeen(true);
                break;
            }
        }
        // Refresh the LiveData to trigger UI updates
        notificationsLiveData.postValue(new ArrayList<>(inMemoryList));

        if (lastEventId != null) {
            notificationsForEventLiveData.postValue((filterByEvent(lastEventId)));
        }
    }

    /**
     * No-operation for the fake repository.
     * Since there are no real Firestore listeners attached, this method does nothing.
     */
    @Override
    public void detachListener() {
        // No-op for fake
    }

    // --- Admin Notif Interface ---
    /**
     * Simulates fetching notifications for a specific event.
     * <p>
     * Filters the local in-memory list by the provided {@code eventId} and updates
     * the target LiveData with the results.
     *
     * @param eventId        The ID of the event to filter by.
     * @param targetLiveData The LiveData to update with the filtered results.
     */
    @Override
    public void fetchNotificationsForEvent(String eventId, MutableLiveData<List<Notification>> targetLiveData) {
        lastEventId = eventId;

        List<Notification> filtered = filterByEvent(eventId);

        targetLiveData.postValue(filtered);
        notificationsForEventLiveData.postValue(filtered);
    }

    // --- Test Helper Methods ---

    /**
     * Helper method to set the initial state of the repository for testing.
     * <p>
     * Use this method in your test's {@code @Before} block to inject a specific
     * list of notifications (e.g., seen, unseen, empty) before the UI launches.
     *
     * @param notifications The list of notifications to load into memory.
     */
    public void setNotifications(List<Notification> notifications) {
        inMemoryList.clear();
        inMemoryList.addAll(notifications);
        notificationsLiveData.postValue(new ArrayList<>(inMemoryList));
    }

    /**
     * Returns the underlying in-memory list of notifications.
     * <p>
     * Useful for assertions in tests to verify that the data was modified correctly
     * (e.g., ensuring a notification's "seen" status was actually flipped).
     *
     * @return The raw list of notifications currently held by the repository.
     */
    public List<Notification> getInMemoryList() {
        return inMemoryList;
    }

    /**
     * Internal helper to filter the in-memory list by event ID.
     */
    private List<Notification> filterByEvent(String eventId) {
        List<Notification> out = new ArrayList<>();

        if (eventId == null) {
            return out;
        }

        for (Notification n : inMemoryList) {
            if (eventId.equals(n.getEventId())) {
                out.add(n);
            }
        }
        return out;
    }

    /**
     * Manually posts a message to the {@code messageLiveData}.
     * <p>
     * Use this in tests to simulate error messages or success toasts triggered
     * by the repository (e.g., "Network Error" or "Success").
     *
     * @param msg The message string to post.
     */
    public void setMessage(String msg) {
        messageLiveData.postValue(msg);
    }

    /**
     * Simulates fetching a user name based on a user ID.
     * <p>
     * For testing purposes, this method returns a predictable name string formatted as
     * "Fake Name for [userId]". If the userId is null, it returns "Unknown ID".
     * This allows UI tests to verify that the name fetching logic is properly connected.
     *
     * @param userId   The ID of the user to look up.
     * @param callback The callback to receive the simulated name.
     */
    @Override
    public void getUserName(String userId, UserNameCallback callback) {
        if (userId == null) {
            callback.onCallback("Unknown ID");
        } else {
            // Return a predictable test string so UI tests can assert the text matches
            callback.onCallback("Fake Name for " + userId);
        }
    }
}