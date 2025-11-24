package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.User;

public interface IUserRepository {

    LiveData<Boolean> isLoading();

    /**
     * gets the notif preference for the user
     * @return notif preference
     */
    LiveData<Boolean> getNotifPreference();
    LiveData<String> getMessage();
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