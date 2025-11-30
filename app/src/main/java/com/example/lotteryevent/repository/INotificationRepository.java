package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.NotificationCustomManager;
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
     * @param notificationCustomManager notification custom manager used to remove notif banners
     */
    void markNotificationAsSeen(String notificationId, NotificationCustomManager notificationCustomManager);

    /**
     * Detaches the real-time Firestore listener to prevent memory leaks.
     * This must be called when the data is no longer needed.
     */
    void detachListener();

    /**
     * Gets the notifications that fall under the organizer for a specified event
     */
    void fetchNotificationsForEvent(String eventId, MutableLiveData<List<Notification>> targetLiveData);

    /**
     * Fetches the display name of a user based on their User ID.
     * <p>
     * This is commonly used in Admin views to translate a recipient ID into a readable name.
     * The result is returned asynchronously via the provided callback.
     *
     * @param userId   The unique ID of the user to look up.
     * @param callback The callback interface to handle the result (name or error string).
     */
    void getUserName(String userId, UserNameCallback callback);

    /**
     * A simple callback interface for handling asynchronous user name retrieval.
     */
    interface UserNameCallback {
        /**
         * Called when the user name lookup is complete.
         *
         * @param name The display name of the user, or a fallback string (e.g., "Unknown User") if not found.
         */
        void onCallback(String name);
    }
}