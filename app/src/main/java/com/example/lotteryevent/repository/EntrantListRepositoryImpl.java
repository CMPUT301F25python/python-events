package com.example.lotteryevent.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.Entrant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
public class EntrantListRepositoryImpl implements IEntrantListRepository{

    private static final String TAG = "EntrantNotifRepo";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    protected final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>();
    private final NotificationCustomManager notifManager;
    private final MutableLiveData<String> _userMessage = new MutableLiveData<>();

    /**
     * Creates a new instance of EntrantListRepository and initializes the custom
     * notification manager used for sending messages to entrants.
     * @param context the application or activity context required for initializing
     *                the NotificationCustomManager
     */
    public EntrantListRepositoryImpl(Context context) {
        notifManager = new NotificationCustomManager(context);
    }

    /**
     * Retrieves the message to display to the user
     * @return _userMessage, the message to the user
     */
    @Override
    public LiveData<String> getUserMessage() {
        return _userMessage;
    }

    /**
     * Allows organizer to write a custom message
     * @param message set by the organizer
     */
    @Override
    public void setUserMessage(String message) {
        _userMessage.postValue(message);
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
     * @param status the status filter (accepted, waiting, cancelled, invited) used to match
     *               entrants in the event's subcollection
     * @return a LiveData stream containing the list of entrants matching the given
     *         status
     */
    @Override
    public LiveData<List<Entrant>> fetchEntrantsByStatus(String eventId, String status) {
        if (eventId == null || status == null) {
            _entrants.postValue(new ArrayList<>());
            _userMessage.postValue("Error: Event data not available.");
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
                    _userMessage.postValue(null); // clearing any previous messages/errors
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
                    _userMessage.postValue("Failed to load entrants.");
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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        db.collection("users").document(currentUser.getUid()).get()
            .addOnSuccessListener(userDoc -> {
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
                                String organizerName = userDoc.getString("name");
                                String title = "Message From Organizer";
                                String message = "Message from the organizer of " + eventName + ": " + organizerMessage;
                                String type = "custom_message";
                                notifManager.sendNotification(uid, title, message, type, eventId, eventName, organizerId, organizerName);
                                Log.d(TAG, "Notification sent successfully to " + uid);
                                _userMessage.postValue("Notification successfully sent.");
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
                            _userMessage.postValue("Failed to send notification.");
                        });
            })
                /**
                 * Logs exception thrown
                 * @param e exception thrown
                 */
            .addOnFailureListener(e -> {
                _userMessage.postValue("Error: Failed to get logged in user.");
                Log.e(TAG, "notifyEntrant failed", e);
            });
    }

    /**
     * Updates the status of a specific entrant (e.g., from "invited" to "waiting").
     *
     * @param eventId   The event ID.
     * @param userId    The entrant's user ID.
     * @param newStatus The new status to set.
     * @param callback  A callback to handle success/failure in the ViewModel.
     * @param sendNotif Boolean indicating whether to send notif to entrant on status update, currently only handles cancellation (new status being "waiting")
     */
    @Override
    public void updateEntrantStatus(String eventId, String userId, String newStatus, StatusUpdateCallback callback, boolean sendNotif) {
        if (eventId == null || userId == null || newStatus == null) {
            if (callback != null) {
                /**
                 * Raises exception on callback failure
                 */
                callback.onFailure(new IllegalArgumentException("Invalid arguments for status update"));
            }
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        db.collection("users").document(currentUser.getUid()).get()
            .addOnSuccessListener(userDoc -> {
                String organizerName = userDoc.getString("name");

                db.collection("events")
                        .document(eventId)
                        .collection("entrants")
                        .document(userId)
                        .update("status", newStatus)
                        /**
                         * Logs and calls callback's specific success behaviour
                         * @param aVoid unusable data
                         */
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Entrant " + userId + " status updated to " + newStatus);

                            if (sendNotif && newStatus.equals("waiting")) {
                                db.collection("events").document(eventId).get()
                                    /**
                                     * Callback triggered when Firestore successfully retrieves the event document
                                     * required for composing the entrant cancellation notification. Extracts the event name,
                                     * organizer ID, and organizer name, constructs a formatted notification, and
                                     * uses NotificationCustomManager to send it to the specified entrant.
                                     * @param document the Firestore DocumentSnapshot representing the event
                                     */
                                    .addOnSuccessListener(document -> {
                                        if (document != null && document.exists()) {
                                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                            String eventName = document.getString("name");
                                            String organizerId = document.getString("organizerId");
                                            String title = "Invitation Update";
                                            String message = "Your invitation to the event " + eventName + " has been withdrawn.";
                                            String type = "custom_message";
                                            notifManager.sendNotification(userId, title, message, type, eventId, eventName, organizerId, organizerName);
                                            Log.d(TAG, "Notification sent successfully to " + userId);
                                            _userMessage.postValue("Notification successfully sent.");
                                        } else {
                                            Log.e(TAG,"Event not found for notification.");
                                        }

                                        if (callback != null) {
                                            callback.onSuccess();
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
                                        _userMessage.postValue("Failed to send notification.");
                                        if (callback != null) {
                                            callback.onFailure(e);
                                        }
                                    });
                            } else {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }
                        })
                        /**
                         * Logs exception thrown
                         * @param e exception thrown
                         */
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update entrant status", e);
                            _userMessage.postValue("Failed to update status.");
                            if (callback != null) {
                                callback.onFailure(e);
                            }
                        });
                    })
            /**
             * Logs exception thrown
             * @param e exception thrown
             */
            .addOnFailureListener(e -> {
                _userMessage.postValue("Error: Failed to get logged in user.");
                Log.e(TAG, "notifyEntrant failed", e);
            });
    }
}
