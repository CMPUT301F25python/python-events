package com.example.lotteryevent.repository;

import android.util.Log;
import android.widget.Toast;

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

public class NotificationRepositoryImpl implements INotificationRepository {
    private static final String TAG = "NotificationRepository";
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<List<Notification>> _notifications = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration; // To manage the real-time listener

    private NotificationCustomManager notifManager;

    public NotificationRepositoryImpl() {
        attachListener();
    }

    @Override
    public LiveData<List<Notification>> getNotifications() {
        return _notifications;
    }

    @Override
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    @Override
    public LiveData<String> getMessage() {
        return _message;
    }

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
     * For each entrant, retrieves even information for notif records, composes message and
     * sends notification
     * @param uid recipient user's ID
     * @param organizerMessage message from organizer
     */
    @Override
    public void notifyEntrant(String uid, String eventId, String organizerMessage) {
        if(uid == null || eventId == null){
            _message.postValue("Invalid notification request.");
            return;
        }

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(document -> {
                    if (document != null && document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        String eventName = document.getString("name");
                        String organizerId = document.getString("organizerId");
                        String organizerName = document.getString("organizerName");
                        String title = "Message From Organizer";
                        String message = "Message from the organizer of " + eventName + ": " + organizerMessage;
                        String type = "custom_message";
                        notifManager.sendNotification(uid, title, message, type, eventId, eventName, organizerId, organizerName);
                    } else {
                        Log.d(TAG, "No such document");
                        Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "get failed with ", e);
                    Toast.makeText(getContext(), "Error sending notification to chosen entrant", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void markNotificationAsSeen(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) return;

        db.collection("notifications").document(notificationId)
                .update("seen", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification " + notificationId + " marked as seen."))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking notification as seen", e);
                    _message.postValue("Failed to update notification status.");
                });
    }

    @Override
    public void detachListener() {
        // This is crucial to prevent memory leaks and stop listening when the view is destroyed.
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}