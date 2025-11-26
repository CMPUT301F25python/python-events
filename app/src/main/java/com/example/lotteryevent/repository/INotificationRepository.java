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
     * Returns a LiveData list of notis under a specified event
     */
    LiveData<List<Notification>> getNotificationsForEvent();

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
     * Gets the notifications that fall under the organizer for a specified event
     */
    void fetchNotificationsForEvent(String eventId);
}