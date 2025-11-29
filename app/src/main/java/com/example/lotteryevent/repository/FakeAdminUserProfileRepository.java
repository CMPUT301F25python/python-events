package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.User;

/**
 * A mock implementation of {@link IAdminUserProfileRepository} used for testing and UI development.
 * <p>
 * This repository bypasses Firestore completely and returns hardcoded "dummy" data immediately.
 * It allows developers to work on the Admin User Profile screen without needing a live database connection
 * or valid user IDs.
 */
public class FakeAdminUserProfileRepository implements IAdminUserProfileRepository {

    /**
     * Simulates fetching a user profile by returning a hardcoded {@link User} object.
     * <p>
     * This method will always trigger {@link UserProfileCallback#onSuccess(User)} immediately.
     *
     * @param userId   The ID requested (used to set the ID of the returned fake user).
     * @param callback The callback to receive the fake user data.
     */
    @Override
    public void getUserProfile(String userId, UserProfileCallback callback) {
        // Create a dummy user
        User fakeUser = new User();
        fakeUser.setId(userId);
        fakeUser.setName("Test User (Fake)");
        fakeUser.setEmail("fake.email@example.com");
        fakeUser.setPhone("555-123-4567");

        // Immediately return success
        if (callback != null) {
            callback.onSuccess(fakeUser);
        }
    }
}
