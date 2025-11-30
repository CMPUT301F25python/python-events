package com.example.lotteryevent.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class UserRepositoryImpl implements IUserRepository {

    private static final String TAG = "UserRepository";
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<User> _currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _notifPreference = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _userMessage = new MutableLiveData<>();

    /**
     * Initializes the repository and sets up an authentication state listener.
     * This listener observes changes in the user's sign-in state.
     * If a user is signed in, it fetches their profile from Firestore.
     * If the user signs out, it updates the current user LiveData to null.
     */
    public UserRepositoryImpl(boolean systemNotifEnabled) {
        mAuth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser != null) {
                fetchFirestoreUserProfile(firebaseUser.getUid(), systemNotifEnabled);
            } else {
                _currentUser.postValue(null);
            }
        });
    }

    /**
     * Fetches a user's profile from Firestore using their UID and their notification preference
     * (based on their preference stored in db as well as device's setting).
     * <p>
     * On success, updates {@code _currentUser} LiveData with the {@link User} object.
     * On failure, posts an error to {@code _userMessage} and logs the exception.
     * The loading state is managed via {@code _isLoading}.
     *
     * @param uid The unique identifier for the user's profile.
     * @param systemNotifEnabled device's system level app notif setting
     */
    private void fetchFirestoreUserProfile(String uid, boolean systemNotifEnabled) {
        _isLoading.setValue(true);
        db.collection("users").document(uid).get()
                /**
                 * Gets user's profile and notif preference
                 * @param task contains user
                 */
                .addOnCompleteListener(task -> {
                    _isLoading.setValue(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        User user = task.getResult().toObject(User.class);
                        _currentUser.postValue(user);
                        if (user != null) {
                            Boolean optOut = user.getOptOutNotifications();
                            if (optOut == null) {
                                optOut = true;
                            }
                            boolean userNotifEnabled = !optOut;
                            fetchNotifPreference(userNotifEnabled, systemNotifEnabled);
                        }
                    } else {
                        _userMessage.postValue("Failed to load user profile.");
                        Log.e(TAG, "Error fetching user profile", task.getException());
                    }
                });
    }

    /**
     * sets notification preference of user depending on the user's db doc's preference
     * and system's preference
     * @param userNotifEnabled user's preference set in db
     * @param systemNotifEnabled device's system level notif preference for the app
     */
    private void fetchNotifPreference(boolean userNotifEnabled, boolean systemNotifEnabled) {
        if (userNotifEnabled && systemNotifEnabled) {
            _notifPreference.postValue(true);
        } else {
            _notifPreference.postValue(false);
        }
    }

    @Override
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    @Override
    public LiveData<Boolean> getNotifPreference() { return _notifPreference; }

    @Override
    public LiveData<String> getMessage() {
        return _userMessage;
    }

    @Override
    public LiveData<User> getCurrentUser() {
        return _currentUser;
    }

    /**
     * Updates the current user's profile in Firestore with the provided user object.
     * It updates LiveData to reflect the operation's progress and result.
     *
     * @param user The {@link User} object with the new profile data.
     */
    @Override
    public void updateUserProfile(User user) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            _userMessage.postValue("No user is signed in to update.");
            return;
        }
        _isLoading.setValue(true);

        db.collection("users").document(firebaseUser.getUid()).get()
            /**
             * Updates user's profile, keeps their notif preference the same
             * @param doc contains user
             */
            .addOnSuccessListener(doc -> {
                Boolean optOutNotifications = doc.getBoolean("optOutNotifications");
                if (optOutNotifications != null) {
                    user.setOptOutNotifications(optOutNotifications);
                }

                db.collection("users").document(firebaseUser.getUid()).set(user)
                    /**
                     * Logs profile update success
                     * @param aVoid unusable data
                     */
                    .addOnSuccessListener(aVoid -> {
                        _isLoading.setValue(false);
                        _currentUser.postValue(user);
                        _userMessage.postValue("Profile updated successfully.");
                        Log.d(TAG, "User profile updated successfully.");
                    })
                    /**
                     * Logs exception thrown
                     * @param e exception thrown
                     */
                    .addOnFailureListener(e -> {
                        _isLoading.setValue(false);
                        _userMessage.postValue("Profile update failed: " + e.getMessage());
                        Log.e(TAG, "Error updating profile", e);
                    });
            })
            /**
             * Logs exception thrown
             * @param e exception thrown
             */
            .addOnFailureListener(e -> Log.e(TAG, "Error getting user", e));
    }

    /**
     * Deletes the current user's data across Firestore.
     * <p>
     * This method performs a multi-step "soft delete" by clearing the user's profile information
     * and removing their associated data from various collections, such as their event history,
     * notifications, event sign-ups, and any events they organized. It does not delete the user
     * document itself.
     * <p>
     * This is a client-side implementation of a complex operation.
     *
     * @see #clearProfileInfo(String)
     * @see #deleteSubcollection(String, String)
     * @see #deleteNotifications(String)
     * @see #deleteFromEventsCol(String)
     * @see #deleteEventsOrganized(String)
     */
    @Override
    public void deleteCurrentUser() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            _userMessage.postValue("No user is signed in to delete.");
            return;
        }
        String uid = firebaseUser.getUid();
        _isLoading.setValue(true);

        // This is a complex, multi-step operation.
        // For a production app, a Firebase Cloud Function is the most robust way to ensure atomicity.
        Task<Void> clearProfileTask = clearProfileInfo(uid);
        Task<Void> deleteHistoryTask = deleteSubcollection(uid, "eventsHistory");
        Task<Void> deleteNotificationsTask = deleteNotifications(uid);
        Task<Void> deleteFromEventsTask = deleteFromEventsCol(uid);
        Task<Void> deleteOrganizedTask = deleteEventsOrganized(uid);

        Tasks.whenAllComplete(clearProfileTask, deleteHistoryTask, deleteNotificationsTask, deleteFromEventsTask, deleteOrganizedTask)
                /**
                 * Logs tasks success or failure
                 * @param task list of tasks completed
                 */
                .addOnCompleteListener(task -> {
                    _isLoading.setValue(false);
                    if (task.isSuccessful()) {
                        _userMessage.postValue("Profile successfully deleted.");
                        Log.d(TAG, "All user data deleted successfully.");
                    } else {
                        _userMessage.postValue("Failed to delete all user data.");
                        Log.e(TAG, "One or more deletion tasks failed.", task.getException());
                    }
                });
    }

    // --- Helper Methods ---

    /**
     * Clears sensitive user data from the Firestore document without deleting it.
     * This retains the user's UID for referential integrity while anonymizing the profile.
     *
     * @param uid The user's unique ID.
     * @return A Task that completes when the profile fields are cleared.
     */
    private Task<Void> clearProfileInfo(String uid) {
        Map<String, Object> clearFields = new HashMap<>();
        clearFields.put("name", null);
        clearFields.put("email", null);
        clearFields.put("phone", null);
        clearFields.put("admin", false);
        clearFields.put("optOutNotifications", true);
        return db.collection("users").document(uid).update(clearFields);
    }

    /**
     * Deletes all documents in a user's subcollection using a batch operation.
     *
     * @param userUid The user's unique ID.
     * @param subcollectionName The name of the subcollection to delete.
     * @return A {@link Task<Void>} that completes when the batch deletion is finished.
     */
    private Task<Void> deleteSubcollection(String userUid, String subcollectionName) {
        return db.collection("users").document(userUid).collection(subcollectionName).get()
                /**
                 * Throws exception if task contains exception, otherwise deletes user's subcollection
                 * @param task contains subcollection of user
                 */
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : task.getResult()) {
                        batch.delete(doc.getReference());
                    }
                    return batch.commit();
                });
    }


    /**
     * Deletes all notifications for a given user.
     *
     * @param uid The user's unique ID.
     * @return A {@link Task<Void>} that completes when the deletion is finished.
     */
    private Task<Void> deleteNotifications(String uid) {
        return db.collection("notifications").whereEqualTo("recipientId", uid).get()
                /**
                 * Throws exception if task contains exception, otherwise deletes notifications
                 * @param task task contains notifications
                 */
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : task.getResult()) {
                        batch.delete(doc.getReference());
                    }
                    return batch.commit();
                });
    }

    /**
     * Removes the user from the "entrants" subcollection of all events.
     *
     * @param uid The user's UID to remove from event entrant lists.
     * @return A {@link Task} that completes upon batch deletion.
     */
    private Task<Void> deleteFromEventsCol(String uid) {
        /**
         * Throws exception if task contains exception, otherwise removed user from "entrants" subcollection of events
         * @param task task contains events
         */
        return db.collection("events").get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            WriteBatch batch = db.batch();
            for (DocumentSnapshot eventDoc : task.getResult()) {
                batch.delete(eventDoc.getReference().collection("entrants").document(uid));
            }
            return batch.commit();
        });
    }

    /**
     * Deletes all events organized by the specified user.
     *
     * @param uid The organizer's user ID.
     * @return A {@link Task} that completes upon deletion.
     */
    private Task<Void> deleteEventsOrganized(String uid) {
        return db.collection("events").whereEqualTo("organizerId", uid).get()
                /**
                 * Throws exception if task contains exception, otherwise deletes events organized by user
                 * @param task task contains events
                 */
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot eventDoc : task.getResult()) {
                        batch.delete(eventDoc.getReference());
                    }
                    return batch.commit();
                });
    }

    /**
     * Updates a user's notif preference in db, clears notifs if now disabled,
     * notifies of missing notifs if db preference and system level preference
     * are both true, and sends error if trying to enable but system level is disabled.
     * @param enabled checkbox value (user's preference for db)
     * @param systemNotifPreference system level notif preference
     * @param notificationCustomManager manager for notifs needed to notify/clear notifs
     */
    @Override
    public void updateNotifPreference(Boolean enabled, boolean systemNotifPreference, NotificationCustomManager notificationCustomManager) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            _userMessage.postValue("No user is signed in to update.");
            return;
        }
        _isLoading.setValue(true);

        db.collection("users").document(firebaseUser.getUid()).update("optOutNotifications", !enabled)
                /**
                 * Removes notif banners if notifs disabled, notifies of any unseen notifs if enabled,
                 * logs change
                 * @param aVoid unusable data
                 */
                .addOnSuccessListener(aVoid -> {
                    _isLoading.setValue(false);
                    fetchNotifPreference(enabled, systemNotifPreference);
                    if (!enabled) {
                        _userMessage.postValue("Notifications disabled.");
                        notificationCustomManager.clearNotifications();
                    } else if (systemNotifPreference) {
                        _userMessage.postValue("Notifications enabled.");
                        notificationCustomManager.checkAndDisplayUnreadNotifications(firebaseUser.getUid());
                    } else {
                        _userMessage.postValue("ERROR: Please allow notifications for the app in settings!");
                    }
                    Log.d(TAG, "Notification preferences updated successfully in db.");
                })
                /**
                 * Logs exception thrown
                 * @param e exception thrown
                 */
                .addOnFailureListener(e -> {
                    _isLoading.setValue(false);
                    _userMessage.postValue("Notification preference update failed: " + e.getMessage());
                    Log.e(TAG, "Error updating notification preference", e);
                });
    }
}