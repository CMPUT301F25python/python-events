package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.Notification;
import java.util.List;

/**
 * Interface for marking notifs as seen and detaching notif listener
 */
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
     * @param notificationCustomManager notification custom manager used to remove notif banners
     */
    void markNotificationAsSeen(String notificationId, NotificationCustomManager notificationCustomManager);

    /**
     * Detaches the real-time Firestore listener to prevent memory leaks.
     * This must be called when the data is no longer needed.
     */
    void detachListener();
}