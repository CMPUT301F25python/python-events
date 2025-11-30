package com.example.lotteryevent.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.utilities.FireStoreUtilities;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of the IEventRepository.
 * This class handles the actual data operations by interacting with Firebase Firestore.
 */
public class EventRepositoryImpl implements IEventRepository {

    private static final String TAG = "EventRepository";

    // Firebase instances
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Private MutableLiveData that will be updated by this repository
    private final MutableLiveData<List<Event>> _events = new MutableLiveData<>();
    private final MutableLiveData<Event> _event = new MutableLiveData<>();
    private final MutableLiveData<Integer> _waitingListCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> _availableSpaceCount = new MutableLiveData<>();
    private final MutableLiveData<String> _organizerName = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _userMessage = new MutableLiveData<>();

    /**

     Returns a LiveData object holding the list of events for the current user.
     The UI can observe this to get real-time updates.
     @return LiveData list of Events.
     */
    @Override
    public LiveData<List<Event>> getUserEvents() {
        return _events;
    }
    /**

     Returns a LiveData object holding a specific event which is specified through calling fetchEventAndEntrants().
     The UI can observe this to get real-time updates.
     @return LiveData Event.
     */
    @Override
    public LiveData<Event> getUserEvent() {
        return _event;
    }
    /**

     Returns a LiveData object holding the count of the number of entrants in the waiting list of a
     specified event which is specified through calling fetchEventAndEntrants().
     The UI can observe this to get real-time updates.
     @return LiveData Event.
     */
    @Override
    public LiveData<Integer> getWaitingListCount() { return _waitingListCount; }
    /**

     Returns a LiveData object holding the count of the number of the number of spaces available of
     a specified event which is specified through calling fetchEventAndEntrants().
     The UI can observe this to get real-time updates.
     @return LiveData Event.
     */
    @Override
    public LiveData<Integer> getAvailableSpaceCount() { return _availableSpaceCount; }
    /**

     Returns a LiveData object holding the current loading state (true if loading, false otherwise).
     @return LiveData Boolean representing the loading state.
     */
    @Override
    public LiveData<String> getOrganizerName() { return _organizerName; }

    @Override
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }
    /**
     Returns a LiveData object holding any messages (success or error) that occur during data fetching.
     @return LiveData String containing an message.
     */
    @Override
    public LiveData<String> getMessage() {
        return _userMessage;
    }

    /**
     Triggers the process of fetching events from the data source for the currently logged-in user.
     */
    @Override
    public void fetchUserEvents() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Cannot fetch events: user is not signed in.");
            _userMessage.setValue("You must be signed in to see your events.");
            _events.setValue(new ArrayList<>()); // Post empty list
            return;
        }

        _isLoading.setValue(true);
        String userId = currentUser.getUid();

        db.collection("events")
                .whereEqualTo("organizerId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                /**
                 * Adds organiser events to list and posts to mutable live data
                 * @param task contains result of query
                 */
                .addOnCompleteListener(task -> {
                    _isLoading.setValue(false);
                    if (task.isSuccessful()) {
                        List<Event> userEvents = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Event event = document.toObject(Event.class);
                            userEvents.add(event);
                        }
                        _events.setValue(userEvents);
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        // Post an error message for the UI to display
                        _userMessage.setValue("Failed to load events. Please check your connection.");
                        _events.setValue(new ArrayList<>()); // Post empty list on error
                    }
                });
    }

    /**
     * Fetches an specified event and its entrants.
     * @param eventId The unique identifier of the event to load.
     */
    @Override
    public void fetchEventAndEntrantCounts(String eventId) {
        _isLoading.postValue(true);

        FirebaseUser currentUser = mAuth.getCurrentUser();

        db.collection("users").document(currentUser.getUid()).get()
            /**
             * Gets organizer name nad fetches entrants count
             * @param doc contains user
             */
            .addOnSuccessListener(doc -> {
                _organizerName.postValue(doc.getString("name"));
                db.collection("events").document(eventId).get()
                    /**
                     * Posts event to mutable live data and fetches entrants count
                     * @param documentSnapshot contains event
                     */
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Event event = documentSnapshot.toObject(Event.class);
                            _event.postValue(event);

                            fetchEntrantsCountsTask(eventId);
                            _isLoading.postValue(false);
                        } else {
                            _isLoading.postValue(false);
                            _userMessage.postValue("Error: Event not found.");
                        }
                    })
                    /**
                     * Logs exception thrown
                     * @param e exception thrown
                     */
                    .addOnFailureListener(e -> {
                        _isLoading.postValue(false);
                        _userMessage.postValue("Error: Failed to load event.");
                        Log.e(TAG, "fetchEvent failed", e);
                    });
            })
            /**
             * Logs exception thrown
             * @param e exception thrown
             */
            .addOnFailureListener(e -> {
                _isLoading.postValue(false);
                _userMessage.postValue("Error: Failed to get logged in user.");
                Log.e(TAG, "fetchEvent failed", e);
            });
    }

    /**
     * Fetches entrant counts of an event
     * @param eventId event to get counts for
     */
    private void fetchEntrantsCountsTask(String eventId) {
        FireStoreUtilities.fillEntrantMetrics(
                db,
                eventId,
                count -> _waitingListCount.postValue(count),
                count -> {},
                count -> _availableSpaceCount.postValue(count),
                msg -> _userMessage.postValue(msg)
        );
    }

    /**
     * Updates an attribute of an entrant of an event
     * @param eventId event to access its entrants
     * @param entrantId entrant's ID in db
     * @param fieldName attribute of entrants to modify
     * @param newValue new value to set
     */
    @Override
    public Task<Void> updateEntrantAttribute(String eventId, String entrantId, String fieldName, Object newValue) {
        DocumentReference entrantRef = db.collection("events").document(eventId).collection("entrants").document(entrantId);
        return entrantRef.update(fieldName, newValue)
                /**
                 * Logs update success
                 * @param query unusable data
                 */
                .addOnSuccessListener(query -> {
                    _userMessage.postValue("Entrant updated successfully");
                })
                /**
                 * Logs failure
                 * @param e exception thrown
                 */
                .addOnFailureListener(e -> _userMessage.postValue("Error updating entrant"));
    }

    /**
     * Creates a new event in Firebase
     * The result (success or failure) will be posted to the message LiveData
     */
    @Override
    public void createEvent(Event event) {
        _isLoading.setValue(true);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Cannot create event: user is not signed in.");
            _userMessage.setValue("You must be signed in to create an event.");
            _isLoading.setValue(false);
            return;
        }
        String userId = currentUser.getUid();
        db.collection("users").document(userId).get()
                /**
                 * Sets event's organizer and saves event to db
                 * @param documentSnapshot contains user
                 */
                .addOnSuccessListener(documentSnapshot -> {
                    String organizerName = "Unknown Organizer";
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        organizerName = documentSnapshot.getString("name");
                    }

                    // Step 2: Set the final details on the event object.
                    event.setOrganizerId(userId);
                    event.setOrganizerName(organizerName);

                    // Step 3: Add the fully formed event to the "events" collection.
                    db.collection("events").add(event)
                            /**
                             * Logs save success
                             * @param documentReference contains reference to doc saved
                             */
                            .addOnSuccessListener(documentReference -> {
                                _isLoading.setValue(false);
                                _userMessage.setValue("Event created successfully!");
                                Log.d(TAG, "Event created with ID: " + documentReference.getId());
                            })
                            /**
                             * Logs exception thrown
                             * @param e exception thrown
                             */
                            .addOnFailureListener(e -> {
                                _isLoading.setValue(false);
                                _userMessage.setValue("Error: Could not save event.");
                                Log.e(TAG, "Error creating event", e);
                            });
                })
                /**
                 * Logs exception thrown
                 * @param e exception thrown
                 */
                .addOnFailureListener(e -> {
                    // This listener catches failures in fetching the user's name.
                    _isLoading.setValue(false);
                    _userMessage.setValue("Error: Could not fetch user details.");
                    Log.e(TAG, "Error fetching organizer name", e);
                });
    }
}