package com.example.lotteryevent.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
import com.google.firebase.firestore.GeoPoint;

import java.util.List;

public class EventDetailsRepositoryImpl implements IEventDetailsRepository {

    private static final String TAG = "EventDetailsRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<Event> _eventDetails = new MutableLiveData<>();
    private final MutableLiveData<Entrant> _entrantStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();
    private final MutableLiveData<Integer> _attendeeCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _waitingListCount = new MutableLiveData<>(0);

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
    public void fetchEventAndEntrantDetails(String eventId) {
        _isLoading.postValue(true);
        db.collection("events").document(eventId).get()
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
                .addOnSuccessListener(doc -> {
                    Entrant entrant = (doc != null && doc.exists()) ? doc.toObject(Entrant.class) : null;
                    _entrantStatus.postValue(entrant);
                });
    }

    private Task<List<Object>> fetchEntrantCountsTask(String eventId) {
        CollectionReference entrantsRef = db.collection("events").document(eventId).collection("entrants");

        // Query 1: Count of "accepted" entrants
        AggregateQuery acceptedCountQuery = entrantsRef.whereEqualTo("status", "accepted").count();
        Task<Long> acceptedTask = acceptedCountQuery.get(AggregateSource.SERVER).onSuccessTask(snapshot -> Tasks.forResult(snapshot.getCount()));

        // Query 2: Count of "waiting" entrants
        AggregateQuery waitingCountQuery = entrantsRef.whereEqualTo("status", "waiting").count();
        Task<Long> waitingTask = waitingCountQuery.get(AggregateSource.SERVER).onSuccessTask(snapshot -> Tasks.forResult(snapshot.getCount()));

        // Run both count queries in parallel and wait for them to succeed.
        return Tasks.whenAllSuccess(acceptedTask, waitingTask)
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
                            .addOnSuccessListener(aVoid -> {
                                _message.postValue("Successfully joined the waiting list!");

                                // Refresh data
                                Task<DocumentSnapshot> entrantStatusTask = fetchEntrantStatusTask(eventId);
                                Task<List<Object>> entrantCountsTask = fetchEntrantCountsTask(eventId);

                                Tasks.whenAllComplete(entrantStatusTask, entrantCountsTask)
                                        .addOnCompleteListener(allTasks -> _isLoading.postValue(false));
                            })
                            .addOnFailureListener(e -> {
                                _isLoading.postValue(false);
                                _message.postValue("Failed to join waiting list.");
                                Log.e(TAG, "joinWaitingList failed to save entrant", e);
                            });

                })
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
                .addOnSuccessListener(aVoid -> {
                    _message.postValue("You have left the event.");
                    Task<DocumentSnapshot> entrantStatusTask = fetchEntrantStatusTask(eventId);
                    Task<List<Object>> entrantCountsTask = fetchEntrantCountsTask(eventId);

                    Tasks.whenAllComplete(entrantStatusTask, entrantCountsTask)
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
                .addOnSuccessListener(aVoid -> {
                    String successMessage = "accepted".equals(newStatus) ? "Invitation accepted!" : "Invitation declined.";
                    _message.postValue(successMessage);
                    Task<DocumentSnapshot> entrantStatusTask = fetchEntrantStatusTask(eventId);
                    Task<List<Object>> entrantCountsTask = fetchEntrantCountsTask(eventId);

                    Tasks.whenAllComplete(entrantStatusTask, entrantCountsTask)
                            .addOnCompleteListener(allTasks -> {
                                _isLoading.postValue(false); // All loading is now finished.
                            });
                })
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
}