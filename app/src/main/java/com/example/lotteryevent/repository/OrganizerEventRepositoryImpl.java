package com.example.lotteryevent.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Event;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class OrganizerEventRepositoryImpl implements IOrganizerEventRepository {
    private static final String TAG = "OrganizerEventRepo";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<Event> _event = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isRunDrawButtonEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();

    @Override
    public LiveData<Event> getEvent() { return _event; }
    @Override
    public LiveData<Boolean> isRunDrawButtonEnabled() { return _isRunDrawButtonEnabled; }
    @Override
    public LiveData<Boolean> isLoading() { return _isLoading; }
    @Override
    public LiveData<String> getMessage() { return _message; }

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
                        _message.postValue("Error: Event not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    _isLoading.postValue(false);
                    _message.postValue("Error: Failed to load event details.");
                    Log.e(TAG, "fetchEventDetails failed", e);
                });
    }

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
                _message.postValue("Could not verify event capacity.");
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