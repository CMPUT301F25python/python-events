package com.example.lotteryevent.viewmodels;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.lotteryevent.data.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the HomeFragment. Handles fetching event data from Firestore.
 */
public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<Event>> _events = new MutableLiveData<>();
    public LiveData<List<Event>> getEvents() {
        return _events;
    }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    /**
     * Fetches events from Firestore where the organizerId matches the current user's ID.
     */
    public void fetchUserEvents() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Cannot fetch events: user is not signed in.");
            _events.setValue(new ArrayList<>());
            return;
        }

        _isLoading.setValue(true);
        String userId = currentUser.getUid();

        db.collection("events")
                .whereEqualTo("organizerId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING) // order by newest first
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
                        _events.setValue(new ArrayList<>()); // Post an empty list on error
                    }
                });
    }
}
