package com.example.lotteryevent.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.User;
import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class EventDetailsRepositoryImpl implements IEventDetailsRepository {

    private static final String TAG = "EventDetailsRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final MutableLiveData<Boolean> _isAdmin = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isDeleted = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isUserDeleted = new MutableLiveData<>(false);

    private final MutableLiveData<Event> _eventDetails = new MutableLiveData<>();
    private final MutableLiveData<Entrant> _entrantStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();
    private final MutableLiveData<Integer> _attendeeCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _waitingListCount = new MutableLiveData<>();

    @Override
    public LiveData<Event> getEventDetails() { return _eventDetails; }
    @Override
    public LiveData<Entrant> getEntrantStatus() { return _entrantStatus; }
    @Override
    public LiveData<Boolean> isLoading() { return _isLoading; }
    @Override
    public LiveData<String> getMessage() { return _message; }
    @Override
    public LiveData<Integer> getAttendeeCount() { return _attendeeCount; }
    @Override
    public LiveData<Integer> getWaitingListCount() { return _waitingListCount; }
    @Override
    public LiveData<Boolean> getIsAdmin() { return _isAdmin; }
    @Override
    public LiveData<Boolean> getIsDeleted() { return _isDeleted; }

    /**
     * Allows user to write a custom message
     * @param message set by the user
     */
    @Override
    public void setMessage(String message) {
        _message.postValue(message);
    }

    @Override
    public void fetchEventAndEntrantDetails(String eventId) {
        _isLoading.postValue(true);
        db.collection("events").document(eventId).get()
                /**
                 * Extracts even from doc and fetches entrant statuses and entrants counts
                 * @param documentSnapshot contains event from db
                 */
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        _eventDetails.postValue(event);

                        // After fetching the event, kick off the subcollection fetches.
                        // We use Tasks.whenAllComplete to know when all of them are done.
                        Task<DocumentSnapshot> entrantStatusTask = fetchEntrantStatusTask(eventId);
                        Task<List<Object>> entrantCountsTask = fetchEntrantCountsTask(eventId);

                        Tasks.whenAllComplete(entrantStatusTask, entrantCountsTask)
                                .addOnCompleteListener(allTasks -> {
                                    _isLoading.postValue(false); // All loading is now finished.
                                });

                    } else {
                        _isLoading.postValue(false);
                        _message.postValue("Error: Event not found.");
                    }
                })
                /**
                 * Logs and messages of exception
                 * @param e exception thrown
                 */
                .addOnFailureListener(e -> {
                    _isLoading.postValue(false);
                    _message.postValue("Error: Failed to load event details.");
                    Log.e(TAG, "fetchEventDetails failed", e);
                });
    }

    private Task<DocumentSnapshot> fetchEntrantStatusTask(String eventId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            _entrantStatus.postValue(null);
            return Tasks.forResult(null); // Return an already completed task.
        }
        return getEntrantDocRef(eventId, currentUser.getUid()).get()
                /**
                 * Extracts entrant from doc and posts to mutable live data
                 * @param doc contains entrant
                 */
                .addOnSuccessListener(doc -> {
                    Entrant entrant = (doc != null && doc.exists()) ? doc.toObject(Entrant.class) : null;
                    _entrantStatus.postValue(entrant);
                });
    }

    private Task<List<Object>> fetchEntrantCountsTask(String eventId) {
        CollectionReference entrantsRef = db.collection("events").document(eventId).collection("entrants");

        // Query 1: Count of "accepted" entrants
        AggregateQuery acceptedCountQuery = entrantsRef.whereEqualTo("status", "accepted").count();
        /**
         * Gets tasks from aggregate query snapshot
         * @param snapshot contains aggregate query result
         */
        Task<Long> acceptedTask = acceptedCountQuery.get(AggregateSource.SERVER).onSuccessTask(snapshot -> Tasks.forResult(snapshot.getCount()));

        // Query 2: Count of "waiting" entrants
        AggregateQuery waitingCountQuery = entrantsRef.whereEqualTo("status", "waiting").count();
        /**
         * Gets tasks from aggregate query snapshot
         * @param snapshot contains aggregate query result
         */
        Task<Long> waitingTask = waitingCountQuery.get(AggregateSource.SERVER).onSuccessTask(snapshot -> Tasks.forResult(snapshot.getCount()));

        // Run both count queries in parallel and wait for them to succeed.
        return Tasks.whenAllSuccess(acceptedTask, waitingTask)
                /**
                 * Posts attendee and waiting list counts to mutable live data
                 * @param results stores counts
                 */
                .addOnSuccessListener(results -> {
                    // results is a List<Object> where results.get(0) is the result of acceptedTask,
                    // and results.get(1) is the result of waitingTask.
                    long attendees = (long) results.get(0);
                    long waiting = (long) results.get(1);

                    _attendeeCount.postValue((int) attendees);
                    _waitingListCount.postValue((int) waiting);
                });
    }

    @Override
    public void joinWaitingList(String eventId, Double latitude, Double longitude) {
        _isLoading.postValue(true);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            _isLoading.postValue(false);
            _message.postValue("You must be signed in to join.");
            return;
        }

        // 1. Fetch the user's profile to get their specific "name" field
        db.collection("users").document(currentUser.getUid()).get()
                /**
                 * Adds user to the waiting list of an event
                 * @param userSnapshot contains user to be added
                 */
                .addOnSuccessListener(userSnapshot -> {

                    // Default to "Anonymous"  if the field is missing
                    String userName = "Anonymous";

                    if (userSnapshot.exists() && userSnapshot.getString("name") != null) {
                        userName = userSnapshot.getString("name");
                    }

                    // 2. Create the Entrant object with the fetched name
                    Entrant newEntrant = new Entrant();
                    newEntrant.setUserName(userName);
                    newEntrant.setStatus("waiting");
                    newEntrant.setDateRegistered(Timestamp.now());

                    // Add location if provided
                    if (latitude != null && longitude != null) {
                        newEntrant.setGeoLocation(new GeoPoint(latitude, longitude));
                    } else {
                        newEntrant.setGeoLocation(null);
                    }

                    // 3. Save the Entrant to the Event's subcollection
                    getEntrantDocRef(eventId, currentUser.getUid()).set(newEntrant)
                            /**
                             * Fetches entrant status and counts as an update after adding entrant to event's waiting list
                             * @param aVoid unusable data
                             */
                            .addOnSuccessListener(aVoid -> {
                                _message.postValue("Successfully joined the waiting list!");

                                // Refresh data
                                Task<DocumentSnapshot> entrantStatusTask = fetchEntrantStatusTask(eventId);
                                Task<List<Object>> entrantCountsTask = fetchEntrantCountsTask(eventId);

                                Tasks.whenAllComplete(entrantStatusTask, entrantCountsTask)
                                        /**
                                         * Sets loading to false
                                         * @param allTasks tasks completed
                                         */
                                        .addOnCompleteListener(allTasks -> _isLoading.postValue(false));
                            })
                            .addOnFailureListener(e -> {
                                _isLoading.postValue(false);
                                _message.postValue("Failed to join waiting list.");
                                Log.e(TAG, "joinWaitingList failed to save entrant", e);
                            });

                })
                /**
                 * Logs exception
                 * @param e exception thrown
                 */
                .addOnFailureListener(e -> {
                    // Failed to fetch the user profile name
                    _isLoading.postValue(false);
                    _message.postValue("Error fetching user profile.");
                    Log.e(TAG, "Failed to fetch user profile for name", e);
                });
    }

    @Override
    public void leaveWaitingList(String eventId) {
        _isLoading.postValue(true);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { /* Handle not logged in */ return; }

        getEntrantDocRef(eventId, currentUser.getUid()).delete()
                /**
                 * Updates entrants statuses and counts after entrant leaves waiting list
                 * @param aVoid unusable data
                 */
                .addOnSuccessListener(aVoid -> {
                    _message.postValue("You have left the event.");
                    Task<DocumentSnapshot> entrantStatusTask = fetchEntrantStatusTask(eventId);
                    Task<List<Object>> entrantCountsTask = fetchEntrantCountsTask(eventId);

                    Tasks.whenAllComplete(entrantStatusTask, entrantCountsTask)
                            /**
                             * Sets loading to false
                             * @param allTasks tasks completed
                             */
                            .addOnCompleteListener(allTasks -> {
                                _isLoading.postValue(false); // All loading is now finished.
                            });
                })
                .addOnFailureListener(e -> {
                    _isLoading.postValue(false);
                    _message.postValue("Failed to leave the event.");
                    Log.e(TAG, "leaveWaitingList failed", e);
                });
    }

    @Override
    public void updateInvitationStatus(String eventId, String newStatus) {
        _isLoading.postValue(true);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { /* Handle not logged in */ return; }

        getEntrantDocRef(eventId, currentUser.getUid()).update("status", newStatus)
                /**
                 * Updates entrant statuses and counts after user accepts/declines inviatation
                 * @param aVoid unusable data
                 */
                .addOnSuccessListener(aVoid -> {
                    String successMessage = "accepted".equals(newStatus) ? "Invitation accepted!" : "Invitation declined.";
                    _message.postValue(successMessage);
                    Task<DocumentSnapshot> entrantStatusTask = fetchEntrantStatusTask(eventId);
                    Task<List<Object>> entrantCountsTask = fetchEntrantCountsTask(eventId);

                    Tasks.whenAllComplete(entrantStatusTask, entrantCountsTask)
                            /**
                             * Sets loading to false
                             * @param allTasks tasks completed
                             */
                            .addOnCompleteListener(allTasks -> {
                                _isLoading.postValue(false); // All loading is now finished.
                            });
                })
                /**
                 * Logs exception
                 * @param e exception thrown
                 */
                .addOnFailureListener(e -> {
                    _isLoading.postValue(false);
                    _message.postValue("Failed to update invitation status.");
                    Log.e(TAG, "updateInvitationStatus failed", e);
                });
    }

    /**
     * Helper method to get a DocumentReference to the current user's entry
     * in the entrants subcollection for a given event.
     */
    private DocumentReference getEntrantDocRef(String eventId, String userId) {
        return db.collection("events").document(eventId).collection("entrants").document(userId);
    }

    /**
     * Verifies if the specified user holds administrative privileges by querying the "users" collection.
     * <p>
     * The result of this check updates the {@link #getIsAdmin()} LiveData observable.
     * If the user ID is null, the document does not exist, or the "admin" field is missing,
     * the status is set to false.
     *
     * @param userId The unique identifier (UID) of the user to verify.
     */
    @Override
    public void checkAdminStatus(String userId) {
        if (userId == null) {
            _isAdmin.postValue(false);
            return;
        }

        db.collection("users").document(userId).get()
                /**
                 * Sets if user is admin or not to mutable live data
                 * @parma documentSnapshot contains user
                 */
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean admin = documentSnapshot.getBoolean("admin");
                        // Update the LiveData so the UI observes the change
                        _isAdmin.postValue(admin != null && admin);
                    } else {
                        _isAdmin.postValue(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "checkAdminStatus failed", e);
                    _isAdmin.postValue(false);
                });
    }

    /**
     * Permanently deletes the specified event and its associated subcollections (e.g., 'entrants').
     * <p>
     * This operation performs a batch write to ensure data consistency:
     * <ol>
     *     <li>Fetches all documents in the 'entrants' subcollection.</li>
     *     <li>Adds delete operations for each entrant to a batch.</li>
     *     <li>Adds the delete operation for the parent event document to the batch.</li>
     *     <li>Commits the batch atomically.</li>
     * </ol>
     * On success, the {@link #getIsDeleted()} LiveData is set to true to trigger navigation.
     *
     * @param eventId The unique identifier of the event to be deleted.
     */
    @Override
    public void deleteEvent(String eventId) {
        _isLoading.postValue(true);

        CollectionReference entrantsRef = db.collection("events").document(eventId).collection("entrants");

        // 1. First, get all entrants
        /**
         * Deletes event and its entrants
         * @param querySnapshot contains entrants of event
         */
        entrantsRef.get().addOnSuccessListener(querySnapshot -> {
            // Create a batch writer
            WriteBatch batch = db.batch();

            // Add every entrant delete to the batch
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                batch.delete(doc.getReference());
            }

            // 2. Add the Parent Event delete to the batch
            DocumentReference eventRef = db.collection("events").document(eventId);
            batch.delete(eventRef);

            // 3. Commit everything at once
            batch.commit()
                    /**
                     * Logs successful deletion
                     * @param aVoid unusable data
                     */
                    .addOnSuccessListener(aVoid -> {
                        _isLoading.postValue(false);
                        _isDeleted.postValue(true);
                        _message.postValue("Event deleted successfully.");
                    })
                    /**
                     * Logs exception thrown
                     * @param e exception thrown
                     */
                    .addOnFailureListener(e -> {
                        _isLoading.postValue(false);
                        _isDeleted.postValue(false);
                        _message.postValue("Failed to delete event data.");
                        Log.e(TAG, "deleteEvent batch failed", e);
                    });

        })
        /**
         * Logs exception thrown
         * @param e exception thrown
         */
        .addOnFailureListener(e -> {
            _isLoading.postValue(false);
            Log.e(TAG, "Failed to fetch entrants for deletion", e);
        });
    }

    /**
     * Permanently removes the specified user (organizer) and all their associated data.
     * <p>
     * This performs a cascade delete operation to ensure database integrity:     * <ol>
     *     <li>Deletes the User profile document.</li>
     *     <li>Deletes all notifications received by this user.</li>
     *     <li><b>Deletes all Events</b> organized by this user (including their entrant subcollections).</li>
     *     <li><b>Removes this user</b> from the waiting lists (entrants) of any other events they joined.</li>
     * </ol>
     *
     * @param userId The unique identifier of the user/organizer to delete.
     */
    @Override
    public void deleteOrganizer(String userId) {
        if (userId == null || userId.isEmpty()) {
            _message.postValue("Invalid User ID.");
            return;
        }
        _isLoading.postValue(true);

        // 1. Delete the User Document
        Task<Void> deleteUserDoc = db.collection("users").document(userId).delete();

        // 2. Delete all Notifications received by this user
        Task<Void> deleteNotifications = deleteUserNotifications(userId);

        // 3. Delete all Events organized by this user (and their subcollections)
        Task<Void> deleteOrganizedEvents = deleteEventsOrganizedByUser(userId);

        // 4. Remove this user from 'entrants' lists of other events
        Task<Void> deleteFromEntrants = removeUserFromAllWaitingLists(userId);

        // Wait for all cleanup tasks to finish
        Tasks.whenAllComplete(deleteUserDoc, deleteNotifications, deleteOrganizedEvents, deleteFromEntrants)
                /**
                 * Logs successful and failed deletions
                 * @param task list of tasks completed
                 */
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    // Check if ANY of the tasks failed
                    if (deleteUserDoc.isSuccessful() && deleteNotifications.isSuccessful()
                            && deleteOrganizedEvents.isSuccessful() && deleteFromEntrants.isSuccessful()) {
                        Log.d(TAG, "Admin successfully deleted user and all associated data: " + userId);
                        _message.postValue("Organizer deleted successfully.");
                        _isUserDeleted.postValue(true);
                    } else {
                        Log.e(TAG, "One or more delete tasks failed.");
                        if (deleteOrganizedEvents.getException() != null) Log.e(TAG, "Events delete failed", deleteOrganizedEvents.getException());
                        if (deleteFromEntrants.getException() != null) Log.e(TAG, "Entrants delete failed", deleteFromEntrants.getException());

                        _message.postValue("Failed to delete user data completely.");
                        _isUserDeleted.postValue(false);
                    }
                });
    }

    @Override
    public LiveData<Boolean> getIsOrganizerDeleted() {
        return _isUserDeleted;
    }

    // --- Helper Methods for User Deletion ---

    /**
     * Deletes all notification documents where 'recipientId' matches the user.
     *
     * @param userId The ID of the user whose notifications should be deleted.
     * @return A Task representing the batch write operation.
     */
    private Task<Void> deleteUserNotifications(String userId) {
        return db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .get()
                /**
                 * Continues the query by deleting all returned documents.
                 *
                 * @param task The completed query task containing documents to delete.
                 * @return A task for committing the batch delete, or a null result if no documents exist.
                 */
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult().isEmpty()) {
                        return Tasks.forResult(null);
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    return batch.commit();
                });
    }

    /**
     * Queries all events where organizerId matches the user and deletes them.
     * <p>
     * Crucially, this method first fetches and queues deletion for the 'entrants' subcollection
     * of every event to ensure no orphaned data remains. It waits for all subcollection
     * fetches to resolve before committing the batch.
     *
     * @param userId The organizer's ID.
     * @return A Task representing the completion of the batch delete.
     */
    private Task<Void> deleteEventsOrganizedByUser(String userId) {
        return db.collection("events")
                .whereEqualTo("organizerId", userId)
                .get()
                /**
                 * Continues the query by deleting each event and its related subcollections.
                 *
                 * @param task The completed query task containing the event documents.
                 * @return A task that commits the batch delete after all subcollection deletions are prepared.
                 */
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult().isEmpty()) {
                        return Tasks.forResult(null);
                    }

                    WriteBatch batch = db.batch();
                    List<Task<Void>> subcollectionTasks = new ArrayList<>();

                    // Iterate through every event organized by this user
                    for (DocumentSnapshot eventDoc : task.getResult().getDocuments()) {
                        // 1. Queue a task to fetch and add entrant deletions to the batch
                        Task<Void> cleanSubcollection = deleteSubcollectionToBatch(eventDoc.getId(), "entrants", batch);
                        subcollectionTasks.add(cleanSubcollection);

                        // 2. Add the deletion of the Event document itself to the batch
                        batch.delete(eventDoc.getReference());
                    }

                    // Wait for all subcollection queries to finish adding to the batch BEFORE committing.
                    return Tasks.whenAllComplete(subcollectionTasks)
                            .continueWithTask(t -> batch.commit());
                });
    }

    /**
     * Helper method that fetches a subcollection and adds delete operations to the provided batch.
     *
     * @param eventId           The parent event ID.
     * @param subcollectionName The name of the subcollection (e.g., "entrants").
     * @param batch             The WriteBatch to add operations to.
     * @return A Task that completes when the documents have been added to the batch.
     */
    private Task<Void> deleteSubcollectionToBatch(String eventId, String subcollectionName, WriteBatch batch) {
        return db.collection("events").document(eventId).collection(subcollectionName).get()
                /**
                 * Continues the task by deleting all documents returned by the query.
                 *
                 * @param task The completed query task containing documents to delete.
                 * @return Always returns null after processing the deletions.
                 */
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            batch.delete(doc.getReference());
                        }
                    }
                    return null;
                });
    }

    /**
     * Removes the user from the waiting list (entrants) of ALL events they have joined.
     * <p>
     * This uses a {@code collectionGroup} query to scan all "entrants" collections.
     * Since the entrants document ID is the User ID, we iterate the results to find matches.
     *
     * @param userId The ID of the user to remove.
     * @return A Task representing the completion of the batch delete.
     */
    private Task<Void> removeUserFromAllWaitingLists(String userId) {
        return db.collectionGroup("entrants")
                .get()
                /**
                 * Continues the query by deleting documents matching the given user ID.
                 *
                 * @param task The completed collection group query task.
                 * @return A task that commits the batch delete if any documents match, or a null result otherwise.
                 */
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Collection Group Query failed", task.getException());
                        return Tasks.forResult(null);
                    }

                    WriteBatch batch = db.batch();
                    boolean hasDeletions = false;

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        if (doc.getId().equals(userId)) {
                            batch.delete(doc.getReference());
                            hasDeletions = true;
                        }
                    }

                    if (hasDeletions) {
                        return batch.commit();
                    } else {
                        return Tasks.forResult(null);
                    }
                });
    }

    /**
     * Allows for admin user to send notifications to users
     * @param userId reciepient user's ID
     * @param adminMessage message to send
     * @param manager notification custom manager used to send message
     */
    @Override
    public void notifyEntrantFromAdmin(String userId, String adminMessage, NotificationCustomManager manager) {
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        if (admin == null) {
            _message.postValue("Admin not signed in.");
            return;
        }
        String senderId = admin.getUid();

        db.collection("users").document(senderId).get()
            /**
             * Sends notification from the admin
             * @param doc contains sender user
             */
            .addOnSuccessListener(doc -> {
                String senderName = doc.getString("name");
                if (senderName == null) {
                    senderName = "Admin";
                }

                String title = "Message From Admin";
                String message = "Message from the admin: " + adminMessage;
                String type = "custom_message";

                manager.sendNotification(userId, title, message, type, null, null, senderId, senderName);
                _message.postValue("Notification Sent!");
            })
            /**
             * Logs exception thrown
             * @param e exception thrown
             */
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to fetch admin user's profile", e);
            });
    }

}