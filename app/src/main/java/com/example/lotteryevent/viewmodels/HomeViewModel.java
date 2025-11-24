package com.example.lotteryevent.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IEventRepository;
import com.example.lotteryevent.ui.HomeFragment;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the Home screen, responsible for preparing and managing the data for the UI.
 * <p>
 * This ViewModel provides the {@link HomeFragment} with the necessary data,
 * such as a list of lottery events, loading status, and error messages. It acts as an intermediary
 * between the UI and the data layer (represented by {@link IEventRepository}).
 * <p>
 * By using a ViewModel, the UI data survives configuration changes (like screen rotations).
 * This class is decoupled from the specific data source implementation, communicating only with the
 * {@link IEventRepository} interface. This design follows the principles of dependency injection,
 * making the ViewModel more testable and maintainable.
 */
public class HomeViewModel extends ViewModel {

    private final IEventRepository eventRepository;

    /**
     * Constructs a HomeViewModel. The repository is "injected" through the constructor,
     * which allows for better testing and follows dependency inversion principles.
     *
     * @param eventRepository An implementation of IEventRepository that this ViewModel will use
     *                        to interact with the data layer.
     */
    public HomeViewModel(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Exposes the LiveData of the list of user-created events from the repository.
     * The UI can observe this LiveData to get real-time updates.
     *
     * @return A LiveData object containing a list of {@link Event}s.
     */
    public LiveData<List<Event>> getEvents() {
        return eventRepository.getUserEvents();
    }

    /**
     * Exposes the loading state from the repository.
     * The UI can observe this to show or hide loading indicators.
     *
     * @return A LiveData object containing a boolean that is true if data is loading, false otherwise.
     */
    public LiveData<Boolean> isLoading() {
        return eventRepository.isLoading();
    }

    /**
     * Exposes  messages from the repository.
     * The UI can observe this to display error/success messages to the user.
     *
     * @return A LiveData object containing a message string.
     */
    public LiveData<String> getMessage() {
        return eventRepository.getMessage();
    }

    /**
     * Called by the UI to request a refresh of the event data.
     * This call is simply delegated to the repository.
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
                            event.setEventId(document.getId());
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
