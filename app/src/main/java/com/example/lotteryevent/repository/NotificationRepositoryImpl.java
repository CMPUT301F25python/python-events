package com.example.lotteryevent.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
    private final MutableLiveData<List<Notification>> _notificationsForEvent = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration; // To manage the real-time listener


    public NotificationRepositoryImpl(Context context) {
        attachListener();
    }

    @Override
    public LiveData<List<Notification>> getNotifications() {
        return _notifications;
    }

    @Override
    public LiveData<List<Notification>> getNotificationsForEvent() { return _notificationsForEvent; }

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

    @Override
    public void fetchNotificationsForEvent(String eventId) {
        _isLoading.postValue(true);

        db.collection("notifications")
                .whereEqualTo("eventId", eventId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    List<Notification> result = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Notification eventNoti = doc.toObject(Notification.class);
                        result.add(eventNoti);
                    }
                    _notificationsForEvent.postValue(result);
                    _isLoading.postValue(false);
                });
    }
}