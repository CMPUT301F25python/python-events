package com.example.lotteryevent.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AvailableEventsRepositoryImpl implements IAvailableEventsRepository {
    private static final String TAG = "AvailableEventRepository";

    // Firebase instances
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Private MutableLiveData that will be updated by this repository
    private final MutableLiveData<List<Event>> _events = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _userMessage = new MutableLiveData<>();
    private ListenerRegistration registration;

    /**
     * Returns a LiveData object holding the list of available events.
     * The UI can observe this to get real-time updates.
     *
     * @return LiveData list of Events.
     */
    @Override
    public LiveData<List<Event>> getAvailableEvents() {
        return _events;
    }

    /**
     * Returns a LiveData object holding the current loading state (true if loading, false otherwise).
     *
     * @return LiveData Boolean representing the loading state.
     */
    @Override
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    /**
     * Returns a LiveData object holding any messages (success or error) that occur during data fetching.
     *
     * @return LiveData String containing an message.
     */
    @Override
    public LiveData<String> getMessage() {
        return _userMessage;
    }

    /**
     * Triggers the process of fetching available events from the data source.
     */
    @Override
    public void fetchAvailableEvents() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Cannot fetch events: user is not signed in.");
            _userMessage.setValue("You must be signed in to see available events.");
            _events.setValue(new ArrayList<>()); // Post empty list
            return;
        }

        _isLoading.setValue(true);

        registration = db.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    _isLoading.setValue(false);

                    if (e != null) {
                        Log.e(TAG, "Error listening for events: ", e);
                        _userMessage.setValue("Failed to load events. Please check your connection.");
                        _events.setValue(new ArrayList<>()); // Post empty list on error
                        return;
                    }

                    if (querySnapshot != null) {
                        Log.d(TAG, "Success: Events fetched: " + querySnapshot.size());
                        List<Event> userEvents = new ArrayList<>();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Event event = document.toObject(Event.class);
                            userEvents.add(event);
                        }
                        _events.setValue(userEvents);
                    } else {
                        // Snapshot is null for some reason, treat as empty
                        _events.setValue(new ArrayList<>());
                    }
                });

    }

    /**
     * Removes the listener used to fetch events from the data source.
     */
    @Override
    public void removeListener() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

}
