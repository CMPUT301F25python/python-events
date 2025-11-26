package com.example.lotteryevent.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;
import java.util.List;

public class OrganizerEventRepositoryImpl implements IOrganizerEventRepository {
    private static final String TAG = "OrganizerEventRepo";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();


    private final MutableLiveData<Event> _event = new MutableLiveData<>();
    private final MutableLiveData<List<Entrant>> _entrants = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isRunDrawButtonEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _userMessage = new MutableLiveData<>();

    @Override
    public LiveData<Event> getEvent() { return _event; }
    @Override
    public LiveData<List<Entrant>> getEntrants() { return _entrants; }
    @Override
    public LiveData<Boolean> isRunDrawButtonEnabled() { return _isRunDrawButtonEnabled; }
    @Override
    public LiveData<Boolean> isLoading() { return _isLoading; }
    @Override
    public LiveData<String> getMessage() { return _userMessage; }

    /**
     * Fetches event details and capacity status from the database.
     * @param eventId the ID of the event to fetch
     */
    @Override
    public void fetchEventAndCapacityStatus(String eventId) {
        _isLoading.postValue(true);
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        _event.postValue(event);

                        // After fetching the event, check the capacity.
                        checkCapacity(eventId, event);
                    } else {
                        _isLoading.postValue(false);
                        _userMessage.postValue("Error: Event not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    _isLoading.postValue(false);
                    _userMessage.postValue("Error: Failed to load event details.");
                    Log.e(TAG, "fetchEventDetails failed", e);
                });
    }

    /**
     * Fetches the full list of entrants for the specific event.
     * Used to generate the CSV file for the entrants.
     * @param eventId the ID of the event
     */
    @Override
    public void fetchEntrants(String eventId) {
        _isLoading.postValue(true);

        db.collection("events").document(eventId).collection("entrants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        // Automatically maps documents to Entrant objects using getters/setters
                        List<Entrant> entrantList = queryDocumentSnapshots.toObjects(Entrant.class);
                        _entrants.postValue(entrantList);
                    }
                    _isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching entrants list", e);
                    _userMessage.postValue("Error: Could not fetch entrants list.");
                    _isLoading.postValue(false);
                });
    }

    /**
     * Finalizes an event by updating its status in the database.
     * @param eventId the ID of the event to finalize
     */
    @Override
    public void finalizeEvent(String eventId) {
        _isLoading.setValue(true);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // 1. Check if user is logged in
        if (currentUser == null) {
            Log.w(TAG, "Cannot finalize event: user is not signed in.");
            _userMessage.setValue("You must be signed in to finalize an event.");
            _isLoading.setValue(false);
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);

        // 2. Fetch the event document first
        eventRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String organizerId = documentSnapshot.getString("organizerId");

                // 3. Compare event organizerId with current user's UID
                if (organizerId != null && organizerId.equals(currentUser.getUid())) {

                    // 4. IDs match, proceed with the update
                    eventRef.update("status", "finalized")
                            .addOnSuccessListener(aVoid -> {
                                _isLoading.setValue(false);
                                _userMessage.setValue("Event finalized successfully!");

                                Event currentEvent = _event.getValue();
                                if (currentEvent != null) {
                                    currentEvent.setStatus("finalized");
                                    _event.postValue(currentEvent); // This triggers the ViewModel observer
                                }

                            })
                            .addOnFailureListener(e -> {
                                _isLoading.setValue(false);
                                _userMessage.setValue("Error: Could not finalize event.");
                                Log.e(TAG, "Error finalizing event", e);
                            });

                } else {
                    // 5. IDs do not match
                    _isLoading.setValue(false);
                    _userMessage.setValue("Permission denied: You are not the organizer of this event.");
                    Log.w(TAG, "finalizeEvent: User " + currentUser.getUid() + " tried to finalize event " + eventId + " belonging to " + organizerId);
                }
            } else {
                // Document doesn't exist
                _isLoading.setValue(false);
                _userMessage.setValue("Error: Event not found.");
            }
        }).addOnFailureListener(e -> {
            // Network error or permission error reading the doc
            _isLoading.setValue(false);
            _userMessage.setValue("Error: Could not verify event ownership.");
            Log.e(TAG, "Error fetching event for verification", e);
        });
    }

    /**
     * Checks the capacity of an event and updates the button state accordingly.
     * If the event has no capacity set, the button is enabled by default.
     * @param eventId the ID of the event to check
     * @param event the Event object to check the capacity of
     */
    private void checkCapacity(String eventId, Event event) {
        if (event == null || event.getCapacity() == null || event.getCapacity() == 0) {
            _isRunDrawButtonEnabled.postValue(true); // Default to enabled if no capacity is set
            _isLoading.postValue(false);
            return;
        }

        Query query = db.collection("events").document(eventId).collection("entrants")
                .whereIn("status", Arrays.asList("invited", "accepted"));

        AggregateQuery countQuery = query.count();
        countQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                long currentCount = task.getResult().getCount();
                // The button is enabled if the count is LESS THAN capacity.
                _isRunDrawButtonEnabled.postValue(currentCount < event.getCapacity());
            } else {
                _isRunDrawButtonEnabled.postValue(true); // Default to enabled on failure
                _userMessage.postValue("Could not verify event capacity.");
            }
            _isLoading.postValue(false); // All loading is now finished.
        });
    }

    @Override
    public void updateEventPoster(String eventId, String posterImageUrl) {
        if (eventId == null || eventId.isEmpty()) {
            Log.e("OrganizerEventRepo", "updateEventPoster: invalid eventId");
            return;
        }

        db.collection("events")
                .document(eventId)
                .update("posterImageUrl", posterImageUrl)
                .addOnSuccessListener(aVoid ->
                        Log.d("OrganizerEventRepo", "Poster updated successfully for event: " + eventId))
                .addOnFailureListener(e ->
                        Log.e("OrganizerEventRepo", "Failed to update poster for event: " + eventId, e));
    }

}