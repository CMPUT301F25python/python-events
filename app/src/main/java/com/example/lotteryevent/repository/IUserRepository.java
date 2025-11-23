package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.User;

public interface IUserRepository {

    LiveData<Boolean> isLoading();
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

    void updateNotifPreference(Boolean enabled, boolean systemNotifPreference, NotificationCustomManager notificationCustomManager);
}