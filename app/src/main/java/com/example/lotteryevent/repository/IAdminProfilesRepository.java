package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.User;

import java.util.List;

/**
 * Interface to allow the fetching of all user profiles from firestore and
 * returning them through callback
 */
public interface IAdminProfilesRepository {
    void getAllProfiles(ProfilesCallback callback);

    /**
     * Callback interface for profile fetching results
     */
    interface ProfilesCallback {
        void onSuccess(List<User> users);
        void onFailure(Exception e);
    }
}
