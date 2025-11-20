package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import com.example.lotteryevent.data.Notification;
import java.util.List;

public interface INotificationRepository {

    /**
     * Returns a LiveData list of notifications for the current user, updated in real-time.
     */
    LiveData<List<Notification>> getNotifications();

    /**
     * Returns the current loading state.
     */
    LiveData<Boolean> isLoading();

    /**
     * Returns any user-facing messages (errors, success confirmations).
     */
    LiveData<String> getMessage();

    /**
     * Marks a specific notification as 'seen' in the database.
     * @param notificationId The ID of the notification to update.
     */
    void markNotificationAsSeen(String notificationId);

    /**
     * Detaches the real-time Firestore listener to prevent memory leaks.
     * This must be called when the data is no longer needed.
     */
    void detachListener();

    /**
     * Fetches event data to write notification
     * @param uid id of the entrant being notified
     * @param eventId id of the event for which entrant is being notified
     * @param organizerMessage contents of the notification
     */
    void notifyEntrant(String uid, String eventId, String organizerMessage);
}