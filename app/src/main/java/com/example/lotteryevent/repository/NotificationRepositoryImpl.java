package com.example.lotteryevent.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements INotificationRepository
 * Allows for marking notifs as seen and detaching notif listener
 */
public class NotificationRepositoryImpl implements INotificationRepository {
    private static final String TAG = "NotificationRepository";
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<List<Notification>> _notifications = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration; // To manage the real-time listener


    /**
     * attaches listener on instance creation
     * @param context context of fragment
     */
    public NotificationRepositoryImpl(Context context) {
        attachListener();
    }

    /**
     * Returns a LiveData list of notifications for the current user, updated in real-time.
     */
    @Override
    public LiveData<List<Notification>> getNotifications() {
        return _notifications;
    }

    /**
     * Returns the current loading state.
     */
    @Override
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    /**
     * Returns any user-facing messages (errors, success confirmations).
     */
    @Override
    public LiveData<String> getMessage() {
        return _message;
    }

    /**
     * Attaches listener to get notifications
     */
    private void attachListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            _message.postValue("You must be signed in to view notifications.");
            return;
        }
        _isLoading.postValue(true);

        // This is the real-time listener
        listenerRegistration = db.collection("notifications")
                .whereEqualTo("recipientId", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                /**
                 * Adds notification of the recipient to a list and posts to mutable live data if successful, otherwise logs exception thrown
                 * @param value contains notif docs
                 * @param error exception thrown
                 */
                .addSnapshotListener((value, error) -> {
                    _isLoading.postValue(false);
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        _message.postValue("Failed to load notifications.");
                        return;
                    }

                    if (value != null) {
                        List<Notification> notificationList = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            notificationList.add(doc.toObject(Notification.class));
                        }
                        _notifications.postValue(notificationList);
                    }
                });
    }

    /**
     * Marks a specific notification as 'seen' in the database.
     * @param notificationId The ID of the notification to update.
     * @param notificationCustomManager notification custom manager used to remove notif banners
     */
    @Override
    public void markNotificationAsSeen(String notificationId, NotificationCustomManager notificationCustomManager) {
        if (notificationId == null || notificationId.isEmpty()) return;

        db.collection("notifications").document(notificationId).get()
                /**
                 * Sets a notif's seen as true and removes its notif banner
                 * @param doc contains notif
                 */
                .addOnSuccessListener(doc -> {

                    Long notifBannerIdLong = doc.getLong("notifBannerId");
                    String recipientId = doc.getString("recipientId");
                    Integer notifBannerId;
                    if (notifBannerIdLong != null) {
                        notifBannerId = notifBannerIdLong.intValue();
                    } else {
                        notifBannerId = null;
                    }

                    // sets notification seen as true in db
                    db.collection("notifications").document(notificationId)
                            .update("seen", true)
                            /**
                             * Removed notif's banner if it exists and all notif banners if all notifs now seen
                             * @param aVoid unusable data
                             */
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Notification " + notificationId + " marked as seen.");

                                // clears the notif's system banner if it exists
                                if (notifBannerId != null) {
                                    notificationCustomManager.clearNotification(notifBannerId);
                                }

                                if (recipientId != null) {
                                    // if all notifs seen, clear all system banners
                                    db.collection("notifications").whereEqualTo("recipientId", recipientId).whereEqualTo("seen", false).get()
                                            /**
                                             * Clears all notif banners
                                             * @param query contains unseen notifs
                                             */
                                            .addOnSuccessListener(query -> {
                                                if (query.isEmpty()) {
                                                    notificationCustomManager.clearNotifications();
                                                }
                                            })
                                            /**
                                             * Logs exception thrown
                                             * @param e exception thrown
                                             */
                                            .addOnFailureListener(e -> Log.e(TAG, "Error getting remaining unseen notifications", e));
                                }
                            })
                            /**
                             * Logs exception thrown
                             * @param e exception thrown
                             */
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error marking notification as seen", e);
                                _message.postValue("Failed to update notification status.");
                            });
                })
                /**
                 * Logs exception thrown
                 * @param e exception thrown
                 */
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch notification", e);
                    _message.postValue("Failed to fetch notification.");
                });
    }

    /**
     * Detaches the real-time Firestore listener to prevent memory leaks.
     * This must be called when the data is no longer needed.
     */
    @Override
    public void detachListener() {
        // This is crucial to prevent memory leaks and stop listening when the view is destroyed.
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}