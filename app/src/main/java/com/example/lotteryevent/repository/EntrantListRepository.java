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

/**
 * This class is responsible for retrieving entrant information for events
 * and sending organizer notifications to individual entrants. It communicates
 * with Firestore to fetch entrant data based on status and uses
 * NotificationCustomManager to send notifications.
 * <p>This repository implements {@link IEntrantListRepository} and exposes
 * LiveData streams so that UI layers can observe entrant list updates.</p>
 * @see IEntrantListRepository
 */
public class EntrantListRepository implements IEntrantListRepository{

    private static final String TAG = "EntrantNotifRepo";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    protected final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>();
    private final NotificationCustomManager notifManager;

    /**
     * Creates a new instance of EntrantListRepository and initializes the custom
     * notification manager used for sending messages to entrants.
     * @param context the application or activity context required for initializing
     *                the NotificationCustomManager
     */
    public EntrantListRepository(Context context) {
        notifManager = new NotificationCustomManager(context);
    }

    /**
     * Retrieves a list of entrants for a given event from Firestore whose
     * {@code status} field matches the specified value. The result is delivered
     * through a LiveData object so that UI components can observe updates
     * asynchronously.
     * <p>If either {@code eventId} or {@code status} is null, an empty list is
     * returned immediately.</p>
     * <p>Firestore Callbacks:</p>
     * <ul>
     *   <li><b>OnSuccessListener</b> — Fired when Firestore successfully returns
     *       matching entrant documents. Converts each document into an
     *       {@link Entrant} object and posts the list to LiveData.</li>
     *   <li><b>OnFailureListener</b> — Fired when Firestore query fails. Logs the
     *       error and posts an empty list to LiveData.</li>
     * </ul>
     * @param eventId the Firestore event document ID for which entrants should be
     *                fetched
     * @param status the status filter (e.g., "open", "finalized") used to match
     *               entrants in the event's subcollection
     * @return a LiveData stream containing the list of entrants matching the given
     *         status
     */
    @Override
    public LiveData<List<Entrant>> fetchEntrantsByStatus(String eventId, String status) {
        if (eventId == null || status == null) {
            _entrants.postValue(new ArrayList<>());
            return _entrants;
        }

        db.collection("events").document(eventId).collection("entrants")
                .whereEqualTo("status", status)
                .get()
                /**
                 * Callback triggered when Firestore successfully retrieves all entrant
                 * documents matching the provided status filter. Iterates through the returned
                 * documents, converts them to {@link Entrant} objects, and updates the LiveData
                 * list. Any null conversions are safely ignored.
                 * @param querySnapshot the Firestore QuerySnapshot containing entrant documents
                 */
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
                /**
                 * Callback triggered when a Firestore error occurs while attempting to fetch
                 * entrants by status. Logs the exception and updates the LiveData with an empty
                 * list so observers can handle the failure gracefully.
                 * @param e the exception thrown during the Firestore fetch operation
                 */
                .addOnFailureListener(e ->{
                    Log.e(TAG, "Failed to load entrants", e);
                    _entrants.postValue(new ArrayList<>());
                });
        return _entrants;
    }

    /**
     * Sends a custom notification message to an individual entrant. Fetches the
     * event's details from Firestore to correctly populate the notification fields
     * (event name, organizer information, message content) before dispatching the
     * notification through {@link NotificationCustomManager}.
     * <p>If {@code uid} or {@code eventId} is null, the request is considered
     * invalid and no notification is sent.</p>
     * <p>Firestore Callbacks:</p>
     * <ul>
     *   <li><b>OnSuccessListener</b> — Retrieves event metadata (name, organizer
     *       ID, organizer name). Constructs the full notification message and sends
     *       it to the specified user.</li>
     *   <li><b>OnFailureListener</b> — Logs any error that occurs when trying to
     *       fetch the event information.</li>
     * </ul>
     * @param uid the unique identifier of the entrant to send the notification to
     * @param eventId the Firestore event ID associated with the notification
     * @param organizerMessage the message written by the organizer to be included
     *                         in the notification
     */
    @Override
    public void notifyEntrant(String uid, String eventId, String organizerMessage) {
        if(uid == null || eventId == null){
            Log.e(TAG,"Invalid notification request.");
            return;
        }

        db.collection("events").document(eventId).get()
                /**
                 * Callback triggered when Firestore successfully retrieves the event document
                 * required for composing the entrant notification. Extracts the event name,
                 * organizer ID, and organizer name, constructs a formatted notification, and
                 * uses NotificationCustomManager to send it to the specified entrant.
                 * @param document the Firestore DocumentSnapshot representing the event
                 */
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
                /**
                 * Callback triggered when Firestore fails to retrieve the event information
                 * needed for sending the notification. Logs the exception so that developers
                 * can diagnose the cause of failure.
                 * @param e the exception thrown during the Firestore document fetch
                 */
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending notification", e);
                });
    }
}
