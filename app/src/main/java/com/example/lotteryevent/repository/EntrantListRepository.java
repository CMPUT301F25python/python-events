package com.example.lotteryevent.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.Entrant;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EntrantListRepository {

    private static final String TAG = "EntrantNotifRepo";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>();
    private final NotificationCustomManager notifManager;

    public EntrantListRepository(Context context) {
        notifManager = new NotificationCustomManager(context);
    }
    /**
     * Fetches entrants from the Firestore subcollection where their 'status' field
     * matches the status passed as an argument to this fragment.
     * Returns a LiveData object holding a list of all entrants for an event based on status.
     * @param eventId id of the event
     * @param status status of the event (open, finalized)
     * @return LiveData<List<Entrant>> for an event
     */
    public LiveData<List<Entrant>> fetchEntrantsByStatus(String eventId, String status) {
        if (eventId == null || status == null) {
            _entrants.postValue(new ArrayList<>());
            return _entrants;
        }

        db.collection("events").document(eventId).collection("entrants")
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Entrant> list = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Entrant entrant = document.toObject(Entrant.class);
                        if (entrant != null) {
                            list.add(entrant);
                        }
                    }
                    _entrants.postValue(list);
                })
                .addOnFailureListener(e ->{
                    Log.e(TAG, "Failed to load entrants", e);
                    _entrants.postValue(new ArrayList<>());
                });
        return _entrants;
    }

    /**
     * For each entrant, retrieves even information for notif records, composes message and
     * sends notification
     * @param uid id of the entrant being notified
     * @param eventId id of the event for which entrant is being notified
     * @param organizerMessage contents of the notification
     */
    public void notifyEntrant(String uid, String eventId, String organizerMessage) {
        if(uid == null || eventId == null){
            Log.e(TAG,"Invalid notification request.");
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
                        Log.d(TAG, "Notification sent successfully to " + uid);
                    } else {
                        Log.e(TAG,"Event not found for notification.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending notification", e);
                });
    }
}
