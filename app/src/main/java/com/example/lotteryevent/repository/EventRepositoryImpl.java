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

    // The ViewModel will observe these public, immutable LiveData objects
    @Override
    public LiveData<List<Event>> getUserEvents() {
        return _events;
    }
    @Override
    public LiveData<Event> getUserEvent() {
        return _event;
    }
    @Override
    public LiveData<Integer> getWaitingListCount() { return _waitingListCount; }
    @Override
    public LiveData<Integer> getAvailableSpaceCount() { return _availableSpaceCount; }
    @Override
    public LiveData<String> getOrganizerName() { return _organizerName; }

    @Override
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }
    @Override
    public LiveData<String> getMessage() {
        return _userMessage;
    }

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

    @Override
    public void fetchEventAndEntrantCounts(String eventId) {
        _isLoading.postValue(true);

        FirebaseUser currentUser = mAuth.getCurrentUser();

        db.collection("users").document(currentUser.getUid()).get()
            .addOnSuccessListener(doc -> {
                _organizerName.postValue(doc.getString("name"));
                db.collection("events").document(eventId).get()
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
                    .addOnFailureListener(e -> {
                        _isLoading.postValue(false);
                        _userMessage.postValue("Error: Failed to load event.");
                        Log.e(TAG, "fetchEvent failed", e);
                    });
            })
            .addOnFailureListener(e -> {
                _isLoading.postValue(false);
                _userMessage.postValue("Error: Failed to get logged in user.");
                Log.e(TAG, "fetchEvent failed", e);
            });
    }

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

    @Override
    public Task<Void> updateEntrantAttribute(String eventId, String entrantId, String fieldName, Object newValue) {
        DocumentReference entrantRef = db.collection("events").document(eventId).collection("entrants").document(entrantId);
        return entrantRef.update(fieldName, newValue)
                .addOnSuccessListener(query -> {
                    _userMessage.postValue("Entrant updated successfully");
                })
                .addOnFailureListener(e -> _userMessage.postValue("Error updating entrant"));
    }

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
                            .addOnSuccessListener(documentReference -> {
                                _isLoading.setValue(false);
                                _userMessage.setValue("Event created successfully!");
                                Log.d(TAG, "Event created with ID: " + documentReference.getId());
                            })
                            .addOnFailureListener(e -> {
                                _isLoading.setValue(false);
                                _userMessage.setValue("Error: Could not save event.");
                                Log.e(TAG, "Error creating event", e);
                            });
                })
                .addOnFailureListener(e -> {
                    // This listener catches failures in fetching the user's name.
                    _isLoading.setValue(false);
                    _userMessage.setValue("Error: Could not fetch user details.");
                    Log.e(TAG, "Error fetching organizer name", e);
                });
    }
}