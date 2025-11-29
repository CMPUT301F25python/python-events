package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.User;

/** * Defines the contract for retrieving detailed user profile information
 * from the data source (e.g., Firestore) for administrative purposes.
 * <p>
 * This interface abstracts the underlying data fetching logic, allowing
 * for easy swapping of implementations (e.g., real Firestore repo vs. fake/test repo).
 */
public interface IAdminUserProfileRepository {

    /**
     * Callback interface to handle the asynchronous results of user profile retrieval.
     */
    interface UserProfileCallback {
        /**
         * Called when the user profile is successfully retrieved.
         *
         * @param user The {@link User} object containing the fetched profile details.
         */
        void onSuccess(User user);

        /**
         * Called when an error occurs during the retrieval process.
         *
         * @param e The exception detailing the cause of the failure (e.g., network error, user not found).
         */
        void onFailure(Exception e);
    }

    /**
     * Retrieves a specific user's profile data based on their unique ID.
     *
     * @param userId   The unique identifier (UID) of the user to fetch.
     * @param callback The {@link UserProfileCallback} to handle the success or failure of the operation.
     * @throws IllegalArgumentException if the provided {@code userId} is null or empty (implementation dependent).
     */
    void getUserProfile(String userId, UserProfileCallback callback);
}