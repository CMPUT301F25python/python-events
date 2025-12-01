package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.User;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Implementation of {@link IAdminUserProfileRepository} that retrieves user data directly from Firebase Firestore.
 * <p>
 * This class connects to the "users" collection in Firestore to fetch profile details
 * for the Administrator's view.
 */
public class AdminUserProfileRepositoryImpl implements IAdminUserProfileRepository {

    private final FirebaseFirestore db;

    /**
     * Default constructor that initializes the Firestore instance.
     */
    public AdminUserProfileRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Asynchronously retrieves a user's profile from the Firestore "users" collection.
     * <p>
     * This method performs the following checks:
     * <ul>
     *     <li>Validates that the provided {@code userId} is not null or empty.</li>
     *     <li>Queries the "users" collection for a document matching the ID.</li>
     *     <li>Deserializes the document into a {@link User} object upon success.</li>
     *     <li>Manually sets the User ID on the returned object to ensure consistency.</li>
     * </ul>
     *
     * @param userId   The unique identifier (document ID) of the user to fetch.
     * @param callback The callback to be invoked with the {@link User} object or an Exception.
     */
    @Override
    public void getUserProfile(String userId, UserProfileCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be empty"));
            return;
        }

        db.collection("users").document(userId).get()
                /**
                 * Call's callback's success with user extracted from the db if successful,
                 * otherwise calls failure behaviour
                 * @param document document containing user
                 */
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setId(userId);
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure(new Exception("Failed to parse user data"));
                        }
                    } else {
                        callback.onFailure(new Exception("User not found"));
                    }
                })
                /**
                 * Calls callback's failure behaviour
                 */
                .addOnFailureListener(callback::onFailure);
    }
}
