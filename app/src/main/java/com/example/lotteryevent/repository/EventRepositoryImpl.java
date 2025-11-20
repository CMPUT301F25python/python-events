package com.example.lotteryevent.repository;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _userMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>();

    // The ViewModel will observe these public, immutable LiveData objects
    @Override
    public LiveData<List<Event>> getUserEvents() {
        return _events;
    }

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

    /**
     * Fetches entrants from the Firestore subcollection where their 'status' field
     * matches the status passed as an argument to this fragment.
     */
    public LiveData<List<Entrant>> fetchEntrantsByStatus(String eventId, String status) {
        if (eventId == null || status == null) {
            _userMessage.postValue("Error: Missing event data.");
            _entrants.setValue(new ArrayList<>());
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
                    _entrants.setValue(list);
                })
                .addOnFailureListener(e ->{
                    Log.e(TAG, "Failed to load entrants", e);
                    _entrants.setValue(new ArrayList<>());
                });
        return _entrants;
    }
}