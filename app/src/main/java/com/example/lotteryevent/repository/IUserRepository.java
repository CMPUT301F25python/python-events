package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.User;

/**
 * Inferace for updating, deleting profile and updating notif preferences
 */
public interface IUserRepository {
    /**

     Returns a LiveData object holding the current loading state (true if loading, false otherwise).
     @return LiveData Boolean representing the loading state.
     */
    LiveData<Boolean> isLoading();

    /**
     * Returns LiveData of notif preferences
     * @return boolean of notif preference
     */
    LiveData<Boolean> getNotifPreference();
    /**
     Returns a LiveData object holding any messages (success or error) that occur during data fetching.
     @return LiveData String containing an message.
     */
    LiveData<String> getMessage();

    /**
     * Returns LiveData of current user
     * @return User
     */
    LiveData<User> getCurrentUser();

    /**
     * Updates the profile for the currently logged-in user.
     * @param user A User object containing the new profile information.
     */
    void updateUserProfile(User user);

    /**
     * Deletes the current user's profile and all associated data.
     */
    void deleteCurrentUser();

    /**
     * Updates a user's notif preference in db, clears notifs if disabled,
     * notifies of missing notifs if db preference and system level preference
     * are both true, and sends error if trying to enable but system level is disabled.
     * @param enabled checkbox value (user's preference for db)
     * @param systemNotifPreference system level notif preference
     * @param notificationCustomManager manager for notifs needed to notify/clear notifs
     */

    void updateNotifPreference(Boolean enabled, boolean systemNotifPreference, NotificationCustomManager notificationCustomManager);
}